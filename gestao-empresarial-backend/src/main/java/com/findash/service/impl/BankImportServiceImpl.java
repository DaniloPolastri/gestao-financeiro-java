package com.findash.service.impl;

import com.findash.dto.bankimport.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.repository.*;
import com.findash.service.BankImportService;
import com.findash.service.parser.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Transactional
public class BankImportServiceImpl implements BankImportService {

    private final BankImportRepository importRepository;
    private final BankImportItemRepository itemRepository;
    private final SupplierMatchRuleRepository matchRuleRepository;
    private final AccountRepository accountRepository;
    private final SupplierRepository supplierRepository;
    private final ClientRepository clientRepository;
    private final CategoryRepository categoryRepository;
    private final OfxParser ofxParser;
    private final CsvParser csvParser;

    public BankImportServiceImpl(BankImportRepository importRepository,
                                 BankImportItemRepository itemRepository,
                                 SupplierMatchRuleRepository matchRuleRepository,
                                 AccountRepository accountRepository,
                                 SupplierRepository supplierRepository,
                                 ClientRepository clientRepository,
                                 CategoryRepository categoryRepository,
                                 OfxParser ofxParser,
                                 CsvParser csvParser) {
        this.importRepository = importRepository;
        this.itemRepository = itemRepository;
        this.matchRuleRepository = matchRuleRepository;
        this.accountRepository = accountRepository;
        this.supplierRepository = supplierRepository;
        this.clientRepository = clientRepository;
        this.categoryRepository = categoryRepository;
        this.ofxParser = ofxParser;
        this.csvParser = csvParser;
    }

    @Override
    public BankImportResponseDTO upload(UUID companyId, UUID userId, MultipartFile file) {
        String filename = file.getOriginalFilename() != null ?
            file.getOriginalFilename() : "extrato";
        BankImportFileType fileType = detectFileType(filename);

        List<ParsedTransaction> transactions;
        try {
            BankStatementParser parser = fileType == BankImportFileType.OFX ? ofxParser : csvParser;
            transactions = parser.parse(file.getInputStream(), filename);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Erro ao processar arquivo: " + e.getMessage());
        }

        if (transactions.isEmpty()) {
            throw new BusinessRuleException("Nenhuma transacao encontrada no arquivo.");
        }

        BankImport bankImport = new BankImport(companyId, filename, fileType, userId);
        bankImport = importRepository.save(bankImport);

        List<SupplierMatchRule> rules = matchRuleRepository.findByCompanyId(companyId);
        List<BankImportItem> items = new ArrayList<>();

        for (ParsedTransaction tx : transactions) {
            BankImportItemType itemType = "CREDIT".equals(tx.type()) ?
                BankImportItemType.CREDIT : BankImportItemType.DEBIT;
            AccountType accountType = itemType == BankImportItemType.DEBIT ?
                AccountType.PAYABLE : AccountType.RECEIVABLE;

            BankImportItem item = new BankImportItem(
                bankImport.getId(), tx.date(), tx.description(),
                tx.amount(), itemType, accountType
            );
            item.setOriginalData(tx.rawData());

            // Deteccao de duplicado
            boolean isDuplicate = accountRepository.existsByCompanyIdAndDueDateAndAmountAndDescription(
                companyId, tx.date(), tx.amount(), tx.description());
            item.setPossibleDuplicate(isDuplicate);

            // Sugestao por regras de matching
            applyMatchingRules(item, rules, tx.description(), companyId);

            items.add(itemRepository.save(item));
        }

        bankImport.setTotalRecords(items.size());
        bankImport = importRepository.save(bankImport);

        return toResponseDTO(bankImport, items, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankImportSummaryDTO> list(UUID companyId) {
        return importRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
            .map(i -> new BankImportSummaryDTO(
                i.getId(), i.getFileName(), i.getFileType().name(),
                i.getStatus().name(), i.getTotalRecords(), i.getCreatedAt()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BankImportResponseDTO getById(UUID companyId, UUID importId) {
        BankImport bankImport = findImportOrThrow(companyId, importId);
        List<BankImportItem> items = itemRepository.findByImportId(importId);
        return toResponseDTO(bankImport, items, companyId);
    }

    @Override
    public BankImportItemResponseDTO updateItem(UUID companyId, UUID importId,
                                               UUID itemId, UpdateImportItemRequestDTO request) {
        BankImport bankImport = findImportOrThrow(companyId, importId);
        assertEditable(bankImport);

        BankImportItem item = itemRepository.findByIdAndImportId(itemId, importId)
            .orElseThrow(() -> new ResourceNotFoundException("Item de importacao", itemId));

        if (request.supplierId() != null) item.setSupplierId(request.supplierId());
        if (request.categoryId() != null) item.setCategoryId(request.categoryId());
        if (request.accountType() != null) item.setAccountType(AccountType.valueOf(request.accountType()));

        item = itemRepository.save(item);
        return toItemDTO(item, companyId);
    }

    @Override
    public List<BankImportItemResponseDTO> updateItemsBatch(UUID companyId, UUID importId,
                                                           BatchUpdateImportItemsRequestDTO request) {
        BankImport bankImport = findImportOrThrow(companyId, importId);
        assertEditable(bankImport);

        List<BankImportItemResponseDTO> result = new ArrayList<>();
        for (UUID itemId : request.itemIds()) {
            itemRepository.findByIdAndImportId(itemId, importId).ifPresent(item -> {
                if (request.supplierId() != null) item.setSupplierId(request.supplierId());
                if (request.categoryId() != null) item.setCategoryId(request.categoryId());
                if (request.accountType() != null) item.setAccountType(AccountType.valueOf(request.accountType()));
                itemRepository.save(item);
                result.add(toItemDTO(item, companyId));
            });
        }
        return result;
    }

    @Override
    public void confirm(UUID companyId, UUID importId) {
        BankImport bankImport = findImportOrThrow(companyId, importId);
        assertEditable(bankImport);

        List<BankImportItem> items = itemRepository.findByImportId(importId);

        long incomplete = items.stream()
            .filter(i -> i.getSupplierId() == null || i.getCategoryId() == null)
            .count();
        if (incomplete > 0) {
            throw new BusinessRuleException(
                incomplete + " item(ns) sem fornecedor ou categoria. Preencha todos antes de confirmar.");
        }

        for (BankImportItem item : items) {
            Account account = new Account(
                companyId, item.getAccountType(),
                item.getDescription(), item.getAmount(),
                item.getDate(), item.getCategoryId()
            );
            UUID counterpartyId = item.getSupplierId();
            if (supplierRepository.existsById(counterpartyId)) {
                account.setSupplierId(counterpartyId);
            } else {
                account.setClientId(counterpartyId);
            }
            accountRepository.save(account);

            // Upsert da regra de matching
            String pattern = normalizePattern(item.getDescription());
            matchRuleRepository.findByCompanyIdAndPattern(companyId, pattern)
                .ifPresentOrElse(
                    rule -> {
                        rule.setSupplierId(item.getSupplierId());
                        rule.setCategoryId(item.getCategoryId());
                        matchRuleRepository.save(rule);
                    },
                    () -> matchRuleRepository.save(
                        new SupplierMatchRule(companyId, pattern, item.getSupplierId(), item.getCategoryId()))
                );
        }

        bankImport.setStatus(BankImportStatus.COMPLETED);
        importRepository.save(bankImport);
    }

    @Override
    public void cancel(UUID companyId, UUID importId) {
        BankImport bankImport = findImportOrThrow(companyId, importId);
        assertEditable(bankImport);
        bankImport.setStatus(BankImportStatus.CANCELLED);
        importRepository.save(bankImport);
        itemRepository.deleteAll(itemRepository.findByImportId(importId));
    }

    // --- Helpers ---

    private BankImportFileType detectFileType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return BankImportFileType.OFX;
        if (lower.endsWith(".csv")) return BankImportFileType.CSV;
        throw new BusinessRuleException("Formato de arquivo nao suportado. Use OFX ou CSV.");
    }

    private void applyMatchingRules(BankImportItem item, List<SupplierMatchRule> rules,
                                    String description, UUID companyId) {
        String descLower = description.toLowerCase();
        for (SupplierMatchRule rule : rules) {
            if (descLower.contains(rule.getPattern().toLowerCase())) {
                item.setSupplierId(rule.getSupplierId());
                if (rule.getCategoryId() != null) item.setCategoryId(rule.getCategoryId());
                return;
            }
        }
        // Fallback: tenta matching por nome de fornecedor/cliente
        applyNameBasedMatching(item, description, companyId);
    }

    private void applyNameBasedMatching(BankImportItem item, String description, UUID companyId) {
        String descLower = description.toLowerCase();
        if (item.getAccountType() == AccountType.PAYABLE) {
            supplierRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .filter(s -> descLower.contains(s.getName().toLowerCase()))
                .findFirst()
                .ifPresent(s -> item.setSupplierId(s.getId()));
        } else {
            clientRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .filter(c -> descLower.contains(c.getName().toLowerCase()))
                .findFirst()
                .ifPresent(c -> item.setSupplierId(c.getId()));
        }
    }

    private String normalizePattern(String description) {
        String[] words = description.trim().split("\\s+");
        return String.join(" ", Arrays.copyOf(words, Math.min(3, words.length))).toLowerCase();
    }

    private BankImport findImportOrThrow(UUID companyId, UUID importId) {
        return importRepository.findByIdAndCompanyId(importId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Importacao", importId));
    }

    private void assertEditable(BankImport bankImport) {
        if (bankImport.getStatus() != BankImportStatus.PENDING_REVIEW) {
            throw new BusinessRuleException(
                "Esta importacao nao pode ser editada pois ja foi confirmada ou cancelada.");
        }
    }

    private BankImportResponseDTO toResponseDTO(BankImport bankImport, List<BankImportItem> items,
                                                UUID companyId) {
        List<BankImportItemResponseDTO> itemDTOs = items.stream()
            .map(item -> toItemDTO(item, companyId))
            .toList();
        return new BankImportResponseDTO(
            bankImport.getId(), bankImport.getFileName(),
            bankImport.getFileType().name(), bankImport.getStatus().name(),
            bankImport.getTotalRecords(), bankImport.getCreatedAt(), itemDTOs
        );
    }

    private BankImportItemResponseDTO toItemDTO(BankImportItem item, UUID companyId) {
        String supplierName = null;
        if (item.getSupplierId() != null) {
            supplierName = supplierRepository.findById(item.getSupplierId())
                .map(s -> s.getName())
                .or(() -> clientRepository.findById(item.getSupplierId()).map(c -> c.getName()))
                .orElse(null);
        }
        String categoryName = null;
        if (item.getCategoryId() != null) {
            categoryName = categoryRepository.findById(item.getCategoryId())
                .map(c -> c.getName()).orElse(null);
        }
        return new BankImportItemResponseDTO(
            item.getId(), item.getDate(), item.getDescription(), item.getAmount(),
            item.getType().name(), item.getAccountType().name(),
            item.getSupplierId(), supplierName,
            item.getCategoryId(), categoryName,
            item.isPossibleDuplicate()
        );
    }
}
