package com.findash.service.impl;

import com.findash.dto.bankimport.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.repository.*;
import com.findash.service.parser.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankImportServiceImplTest {

    @Mock private BankImportRepository importRepository;
    @Mock private BankImportItemRepository itemRepository;
    @Mock private SupplierMatchRuleRepository matchRuleRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OfxParser ofxParser;
    @Mock private CsvParser csvParser;
    @Mock private PdfParser pdfParser;

    private BankImportServiceImpl service;
    private UUID companyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new BankImportServiceImpl(importRepository, itemRepository,
            matchRuleRepository, accountRepository, supplierRepository,
            clientRepository, categoryRepository, ofxParser, csvParser, pdfParser);
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void upload_validOfx_createsImportAndItems() throws Exception {
        var file = new MockMultipartFile("file", "extrato.ofx", "application/octet-stream",
            "dummy".getBytes());
        var tx = new ParsedTransaction(LocalDate.now(), "XPTO", new BigDecimal("100"), "DEBIT", Map.of());

        when(ofxParser.parse(any(), any())).thenReturn(new ParseResult(List.of(tx), null));
        when(matchRuleRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(supplierRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of());
        when(accountRepository.existsByCompanyIdAndDueDateAndAmountAndDescription(any(), any(), any(), any()))
            .thenReturn(false);

        BankImport savedImport = new BankImport(companyId, "extrato.ofx", BankImportFileType.OFX, userId);
        savedImport.setId(UUID.randomUUID());
        when(importRepository.save(any())).thenReturn(savedImport);

        BankImportItem savedItem = new BankImportItem(savedImport.getId(), LocalDate.now(),
            "XPTO", new BigDecimal("100"), BankImportItemType.DEBIT, AccountType.PAYABLE);
        savedItem.setId(UUID.randomUUID());
        when(itemRepository.save(any())).thenReturn(savedItem);

        BankImportResponseDTO result = service.upload(companyId, userId, file);
        assertNotNull(result);
        verify(importRepository, times(2)).save(any());
        verify(itemRepository, times(1)).save(any());
    }

    @Test
    void upload_emptyFile_throws() throws Exception {
        var file = new MockMultipartFile("file", "extrato.ofx", "application/octet-stream", "dummy".getBytes());
        when(ofxParser.parse(any(), any())).thenReturn(new ParseResult(List.of(), null));
        assertThrows(BusinessRuleException.class, () -> service.upload(companyId, userId, file));
    }

    @Test
    void upload_unsupportedExtension_throws() {
        var file = new MockMultipartFile("file", "extrato.xls", "application/octet-stream", "dummy".getBytes());
        assertThrows(BusinessRuleException.class, () -> service.upload(companyId, userId, file));
    }

    @Test
    void confirm_withIncompleteItems_throws() {
        UUID importId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);

        BankImportItem item = new BankImportItem(importId, LocalDate.now(), "X",
            BigDecimal.TEN, BankImportItemType.DEBIT, AccountType.PAYABLE);
        // sem supplierId e categoryId

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));
        when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));

        assertThrows(BusinessRuleException.class, () -> service.confirm(companyId, importId));
    }

    @Test
    void cancel_pendingImport_setsStatusCancelled() {
        UUID importId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));
        when(itemRepository.findByImportId(importId)).thenReturn(List.of());

        service.cancel(companyId, importId);

        assertEquals(BankImportStatus.CANCELLED, bankImport.getStatus());
        verify(importRepository).save(bankImport);
    }

    @Test
    void updateItem_completedImport_throws() {
        UUID importId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);
        bankImport.setStatus(BankImportStatus.COMPLETED);

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));

        assertThrows(BusinessRuleException.class,
            () -> service.updateItem(companyId, importId, UUID.randomUUID(),
                new UpdateImportItemRequestDTO(null, null, null)));
    }

    @Test
    void confirm_pastDateItem_createsAccountAsPaid() {
        UUID importId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);

        LocalDate pastDate = LocalDate.now().minusDays(5);
        BankImportItem item = new BankImportItem(importId, pastDate, "Pix enviado",
            new BigDecimal("100.00"), BankImportItemType.DEBIT, AccountType.PAYABLE);
        item.setId(UUID.randomUUID());
        item.setSupplierId(supplierId);
        item.setCategoryId(categoryId);

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));
        when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
        when(supplierRepository.existsById(supplierId)).thenReturn(true);
        when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
            .thenReturn(Optional.empty());
        when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        service.confirm(companyId, importId);

        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.PAID &&
            account.getPaymentDate() != null &&
            account.getPaymentDate().equals(pastDate)
        ));
    }

    @Test
    void confirm_futureDateItem_createsAccountAsPending() {
        UUID importId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);

        LocalDate futureDate = LocalDate.now().plusDays(5);
        BankImportItem item = new BankImportItem(importId, futureDate, "Agendamento",
            new BigDecimal("200.00"), BankImportItemType.DEBIT, AccountType.PAYABLE);
        item.setId(UUID.randomUUID());
        item.setSupplierId(supplierId);
        item.setCategoryId(categoryId);

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));
        when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
        when(supplierRepository.existsById(supplierId)).thenReturn(true);
        when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
            .thenReturn(Optional.empty());
        when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        service.confirm(companyId, importId);

        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.PENDING &&
            account.getPaymentDate() == null
        ));
    }

    @Test
    void confirm_pastDateReceivable_createsAccountAsReceived() {
        UUID importId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
        bankImport.setId(importId);

        LocalDate pastDate = LocalDate.now().minusDays(3);
        BankImportItem item = new BankImportItem(importId, pastDate, "Recebimento",
            new BigDecimal("500.00"), BankImportItemType.CREDIT, AccountType.RECEIVABLE);
        item.setId(UUID.randomUUID());
        item.setSupplierId(clientId);
        item.setCategoryId(categoryId);

        when(importRepository.findByIdAndCompanyId(importId, companyId))
            .thenReturn(Optional.of(bankImport));
        when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
        when(supplierRepository.existsById(clientId)).thenReturn(false);
        when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
            .thenReturn(Optional.empty());
        when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        service.confirm(companyId, importId);

        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.RECEIVED &&
            account.getPaymentDate() != null
        ));
    }
}
