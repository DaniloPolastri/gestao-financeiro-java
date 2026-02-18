package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.AccountMapper;
import com.findash.repository.*;
import com.findash.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final int MAX_RECURRENCE_ENTRIES = 60;

    private final AccountRepository accountRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ClientRepository clientRepository;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(AccountRepository accountRepository,
                              RecurrenceRepository recurrenceRepository,
                              CategoryRepository categoryRepository,
                              SupplierRepository supplierRepository,
                              ClientRepository clientRepository,
                              AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.recurrenceRepository = recurrenceRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.clientRepository = clientRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public AccountResponseDTO create(UUID companyId, CreateAccountRequestDTO request) {
        AccountType type = AccountType.valueOf(request.type());

        Category category = categoryRepository.findByIdAndCompanyId(request.categoryId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

        Supplier supplier = null;
        Client client = null;

        if (type == AccountType.PAYABLE) {
            if (request.supplierId() == null) {
                throw new BusinessRuleException("Fornecedor e obrigatorio para contas a pagar");
            }
            supplier = supplierRepository.findByIdAndCompanyId(request.supplierId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", request.supplierId()));
        } else {
            if (request.clientId() == null) {
                throw new BusinessRuleException("Cliente e obrigatorio para contas a receber");
            }
            client = clientRepository.findByIdAndCompanyId(request.clientId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", request.clientId()));
        }

        if (request.recurrence() != null) {
            return createWithRecurrence(companyId, type, request, category, supplier, client);
        }

        Account account = buildAccount(companyId, type, request, null);
        account = accountRepository.save(account);
        return accountMapper.toResponse(account, category, supplier, client);
    }

    private AccountResponseDTO createWithRecurrence(UUID companyId, AccountType type,
                                                     CreateAccountRequestDTO request,
                                                     Category category, Supplier supplier, Client client) {
        RecurrenceRequestDTO recDto = request.recurrence();
        RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(recDto.frequency());

        Recurrence recurrence = new Recurrence(companyId, frequency, request.dueDate());
        recurrence.setEndDate(recDto.endDate());
        recurrence.setMaxOccurrences(recDto.maxOccurrences());
        recurrence = recurrenceRepository.save(recurrence);

        List<LocalDate> dates = generateDates(request.dueDate(), frequency, recDto.endDate(), recDto.maxOccurrences());

        Account firstAccount = null;
        for (LocalDate date : dates) {
            Account account = buildAccount(companyId, type, request, recurrence.getId());
            account.setDueDate(date);
            account = accountRepository.save(account);
            if (firstAccount == null) firstAccount = account;
        }

        return accountMapper.toResponse(firstAccount, category, supplier, client);
    }

    private Account buildAccount(UUID companyId, AccountType type, CreateAccountRequestDTO request, UUID recurrenceId) {
        Account account = new Account(companyId, type, request.description().trim(),
            request.amount(), request.dueDate(), request.categoryId());
        account.setSupplierId(request.supplierId());
        account.setClientId(request.clientId());
        account.setRecurrenceId(recurrenceId);
        account.setNotes(request.notes());
        return account;
    }

    private List<LocalDate> generateDates(LocalDate startDate, RecurrenceFrequency frequency,
                                           LocalDate endDate, Integer maxOccurrences) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        int count = 0;

        while (count < MAX_RECURRENCE_ENTRIES) {
            if (endDate != null && current.isAfter(endDate)) break;
            if (maxOccurrences != null && count >= maxOccurrences) break;

            dates.add(current);
            count++;

            current = switch (frequency) {
                case MONTHLY -> current.plusMonths(1);
                case WEEKLY -> current.plusWeeks(1);
                case BIWEEKLY -> current.plusWeeks(2);
                case YEARLY -> current.plusYears(1);
            };
        }

        return dates;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDTO getById(UUID companyId, UUID accountId) {
        Account account = findOrThrow(companyId, accountId);
        return toResponseWithRelations(account);
    }

    @Override
    public AccountResponseDTO update(UUID companyId, UUID accountId, UpdateAccountRequestDTO request) {
        Account account = findOrThrow(companyId, accountId);

        categoryRepository.findByIdAndCompanyId(request.categoryId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

        account.setDescription(request.description().trim());
        account.setAmount(request.amount());
        account.setDueDate(request.dueDate());
        account.setCategoryId(request.categoryId());
        account.setSupplierId(request.supplierId());
        account.setClientId(request.clientId());
        account.setNotes(request.notes());

        account = accountRepository.save(account);
        return toResponseWithRelations(account);
    }

    @Override
    public AccountResponseDTO pay(UUID companyId, UUID accountId, PayAccountRequestDTO request) {
        Account account = findOrThrow(companyId, accountId);

        if (account.getStatus() == AccountStatus.PAID || account.getStatus() == AccountStatus.RECEIVED) {
            throw new BusinessRuleException("Esta conta ja foi paga/recebida");
        }

        account.setPaymentDate(request.paymentDate());

        if (request.amountPaid() != null && request.amountPaid().compareTo(account.getAmount()) < 0) {
            account.setStatus(AccountStatus.PARTIAL);
        } else {
            account.setStatus(account.getType() == AccountType.PAYABLE ? AccountStatus.PAID : AccountStatus.RECEIVED);
        }

        account = accountRepository.save(account);
        return toResponseWithRelations(account);
    }

    @Override
    public void delete(UUID companyId, UUID accountId) {
        Account account = findOrThrow(companyId, accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponseDTO> list(UUID companyId, AccountType type, List<AccountStatus> statuses,
                                          UUID categoryId, UUID supplierId, UUID clientId,
                                          LocalDate dueDateFrom, LocalDate dueDateTo, Pageable pageable) {
        Specification<Account> spec = Specification.where(AccountSpecifications.isActive())
            .and(AccountSpecifications.hasCompanyId(companyId))
            .and(AccountSpecifications.hasType(type));

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(AccountSpecifications.hasStatusIn(statuses));
        }
        if (categoryId != null) {
            spec = spec.and(AccountSpecifications.hasCategoryId(categoryId));
        }
        if (supplierId != null) {
            spec = spec.and(AccountSpecifications.hasSupplierId(supplierId));
        }
        if (clientId != null) {
            spec = spec.and(AccountSpecifications.hasClientId(clientId));
        }
        if (dueDateFrom != null) {
            spec = spec.and(AccountSpecifications.dueDateFrom(dueDateFrom));
        }
        if (dueDateTo != null) {
            spec = spec.and(AccountSpecifications.dueDateTo(dueDateTo));
        }

        return accountRepository.findAll(spec, pageable).map(this::toResponseWithRelations);
    }

    private AccountResponseDTO toResponseWithRelations(Account account) {
        Category category = account.getCategoryId() != null
            ? categoryRepository.findById(account.getCategoryId()).orElse(null)
            : null;
        Supplier supplier = account.getSupplierId() != null
            ? supplierRepository.findById(account.getSupplierId()).orElse(null)
            : null;
        Client client = account.getClientId() != null
            ? clientRepository.findById(account.getClientId()).orElse(null)
            : null;
        return accountMapper.toResponse(account, category, supplier, client);
    }

    private Account findOrThrow(UUID companyId, UUID accountId) {
        return accountRepository.findByIdAndCompanyId(accountId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Conta", accountId));
    }
}
