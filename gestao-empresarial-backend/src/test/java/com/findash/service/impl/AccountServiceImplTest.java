package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.AccountMapper;
import com.findash.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private RecurrenceRepository recurrenceRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AccountMapper accountMapper;

    private AccountServiceImpl accountService;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(
            accountRepository, recurrenceRepository,
            categoryRepository, supplierRepository, clientRepository,
            accountMapper
        );
        companyId = UUID.randomUUID();
    }

    // --- CREATE ---

    @Test
    void createPayable_success() {
        UUID categoryId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        var request = new CreateAccountRequestDTO(
            "PAYABLE", "Aluguel", new BigDecimal("2500.00"),
            LocalDate.of(2026, 3, 10), categoryId, supplierId, null, "Ref marco", null
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId))
            .thenReturn(Optional.of(new Category(UUID.randomUUID(), companyId, "Operacional")));
        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId))
            .thenReturn(Optional.of(new Supplier(companyId, "Imobiliaria")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PENDING"));

        AccountResponseDTO result = accountService.create(companyId, request);

        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createPayable_withoutSupplier_throws() {
        UUID categoryId = UUID.randomUUID();
        var request = new CreateAccountRequestDTO(
            "PAYABLE", "Aluguel", new BigDecimal("2500.00"),
            LocalDate.of(2026, 3, 10), categoryId, null, null, null, null
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId))
            .thenReturn(Optional.of(new Category(UUID.randomUUID(), companyId, "Operacional")));

        assertThrows(BusinessRuleException.class, () -> accountService.create(companyId, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createReceivable_withoutClient_throws() {
        UUID categoryId = UUID.randomUUID();
        var request = new CreateAccountRequestDTO(
            "RECEIVABLE", "Consultoria", new BigDecimal("4500.00"),
            LocalDate.of(2026, 3, 15), categoryId, null, null, null, null
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId))
            .thenReturn(Optional.of(new Category(UUID.randomUUID(), companyId, "Servicos")));

        assertThrows(BusinessRuleException.class, () -> accountService.create(companyId, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createPayable_invalidCategory_throws() {
        UUID categoryId = UUID.randomUUID();
        var request = new CreateAccountRequestDTO(
            "PAYABLE", "Aluguel", new BigDecimal("2500.00"),
            LocalDate.of(2026, 3, 10), categoryId, UUID.randomUUID(), null, null, null
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.create(companyId, request));
    }

    @Test
    void createWithRecurrence_generatesMultipleEntries() {
        UUID categoryId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        var recurrence = new RecurrenceRequestDTO("MONTHLY", LocalDate.of(2026, 6, 10), null);
        var request = new CreateAccountRequestDTO(
            "PAYABLE", "Aluguel", new BigDecimal("2500.00"),
            LocalDate.of(2026, 3, 10), categoryId, supplierId, null, null, recurrence
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId))
            .thenReturn(Optional.of(new Category(UUID.randomUUID(), companyId, "Operacional")));
        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId))
            .thenReturn(Optional.of(new Supplier(companyId, "Imobiliaria")));
        when(recurrenceRepository.save(any(Recurrence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PENDING"));

        accountService.create(companyId, request);

        // March, April, May, June = 4 entries
        verify(accountRepository, times(4)).save(any(Account.class));
        verify(recurrenceRepository).save(any(Recurrence.class));
    }

    @Test
    void createPayable_withPastDueDate_setsOverdueStatus() {
        UUID categoryId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        var request = new CreateAccountRequestDTO(
            "PAYABLE", "Aluguel Atrasado", new BigDecimal("2500.00"),
            LocalDate.now().minusDays(3), categoryId, supplierId, null, null, null
        );

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId))
            .thenReturn(Optional.of(new Category(UUID.randomUUID(), companyId, "Operacional")));
        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId))
            .thenReturn(Optional.of(new Supplier(companyId, "Imobiliaria")));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "OVERDUE"));

        accountService.create(companyId, request);

        verify(accountRepository).save(argThat(account -> account.getStatus() == AccountStatus.OVERDUE));
    }

    // --- PAY ---

    @Test
    void payPayable_fullPayment_setsPaid() {
        UUID accountId = UUID.randomUUID();
        Account account = createMockAccount(accountId, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("1000.00"));
        var payRequest = new PayAccountRequestDTO(LocalDate.of(2026, 3, 10), null);

        when(accountRepository.findByIdAndCompanyId(accountId, companyId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PAID"));

        accountService.pay(companyId, accountId, payRequest);

        assertEquals(AccountStatus.PAID, account.getStatus());
        assertEquals(LocalDate.of(2026, 3, 10), account.getPaymentDate());
    }

    @Test
    void payReceivable_fullPayment_setsReceived() {
        UUID accountId = UUID.randomUUID();
        Account account = createMockAccount(accountId, AccountType.RECEIVABLE, AccountStatus.PENDING, new BigDecimal("4500.00"));
        var payRequest = new PayAccountRequestDTO(LocalDate.of(2026, 3, 10), null);

        when(accountRepository.findByIdAndCompanyId(accountId, companyId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("RECEIVABLE", "RECEIVED"));

        accountService.pay(companyId, accountId, payRequest);

        assertEquals(AccountStatus.RECEIVED, account.getStatus());
    }

    @Test
    void payPartial_setsPartialStatus() {
        UUID accountId = UUID.randomUUID();
        Account account = createMockAccount(accountId, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("1000.00"));
        var payRequest = new PayAccountRequestDTO(LocalDate.of(2026, 3, 10), new BigDecimal("500.00"));

        when(accountRepository.findByIdAndCompanyId(accountId, companyId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PARTIAL"));

        accountService.pay(companyId, accountId, payRequest);

        assertEquals(AccountStatus.PARTIAL, account.getStatus());
    }

    @Test
    void payAlreadyPaid_throws() {
        UUID accountId = UUID.randomUUID();
        Account account = createMockAccount(accountId, AccountType.PAYABLE, AccountStatus.PAID, new BigDecimal("1000.00"));
        var payRequest = new PayAccountRequestDTO(LocalDate.of(2026, 3, 10), null);

        when(accountRepository.findByIdAndCompanyId(accountId, companyId)).thenReturn(Optional.of(account));

        assertThrows(BusinessRuleException.class, () -> accountService.pay(companyId, accountId, payRequest));
        verify(accountRepository, never()).save(any());
    }

    // --- DELETE ---

    @Test
    void delete_hardDeletes() {
        UUID accountId = UUID.randomUUID();
        Account account = createMockAccount(accountId, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("1000.00"));

        when(accountRepository.findByIdAndCompanyId(accountId, companyId)).thenReturn(Optional.of(account));

        accountService.delete(companyId, accountId);

        verify(accountRepository).delete(account);
    }

    // --- LIST ---

    @Test
    @SuppressWarnings("unchecked")
    void list_returnsPagedResults() {
        Account account = createMockAccount(UUID.randomUUID(), AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("100.00"));
        Page<Account> page = new PageImpl<>(List.of(account));
        Pageable pageable = PageRequest.of(0, 20);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PENDING"));

        Page<AccountResponseDTO> result = accountService.list(companyId, AccountType.PAYABLE, null, null, null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // --- BATCH PAY ---

    @Test
    void batchPay_marksPendingAccountsAsPaid() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Account acc1 = createMockAccount(id1, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("100.00"));
        Account acc2 = createMockAccount(id2, AccountType.PAYABLE, AccountStatus.OVERDUE, new BigDecimal("200.00"));
        var request = new BatchPayRequestDTO(List.of(id1, id2), LocalDate.of(2026, 2, 19));

        when(accountRepository.findByIdInAndCompanyId(List.of(id1, id2), companyId))
            .thenReturn(List.of(acc1, acc2));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PAID"));

        List<AccountResponseDTO> result = accountService.batchPay(companyId, request);

        assertEquals(2, result.size());
        assertEquals(AccountStatus.PAID, acc1.getStatus());
        assertEquals(AccountStatus.PAID, acc2.getStatus());
        assertEquals(LocalDate.of(2026, 2, 19), acc1.getPaymentDate());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void batchPay_skipsAlreadyPaidAccounts() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Account acc1 = createMockAccount(id1, AccountType.PAYABLE, AccountStatus.PAID, new BigDecimal("100.00"));
        Account acc2 = createMockAccount(id2, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("200.00"));
        var request = new BatchPayRequestDTO(List.of(id1, id2), LocalDate.of(2026, 2, 19));

        when(accountRepository.findByIdInAndCompanyId(List.of(id1, id2), companyId))
            .thenReturn(List.of(acc1, acc2));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("PAYABLE", "PAID"));

        List<AccountResponseDTO> result = accountService.batchPay(companyId, request);

        assertEquals(1, result.size());
        assertEquals(AccountStatus.PAID, acc1.getStatus()); // unchanged
        verify(accountRepository, times(1)).save(any(Account.class)); // only acc2
    }

    @Test
    void batchPay_receivable_setsReceivedStatus() {
        UUID id1 = UUID.randomUUID();
        Account acc1 = createMockAccount(id1, AccountType.RECEIVABLE, AccountStatus.PENDING, new BigDecimal("500.00"));
        var request = new BatchPayRequestDTO(List.of(id1), LocalDate.of(2026, 2, 19));

        when(accountRepository.findByIdInAndCompanyId(List.of(id1), companyId))
            .thenReturn(List.of(acc1));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountMapper.toResponse(any(), any(), any(), any()))
            .thenReturn(createMockResponse("RECEIVABLE", "RECEIVED"));

        accountService.batchPay(companyId, request);

        assertEquals(AccountStatus.RECEIVED, acc1.getStatus());
    }

    // --- HELPERS ---

    private Account createMockAccount(UUID id, AccountType type, AccountStatus status, BigDecimal amount) {
        Account account = new Account(companyId, type, "Test", amount, LocalDate.now(), UUID.randomUUID());
        account.setStatus(status);
        try {
            var idField = account.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return account;
    }

    private AccountResponseDTO createMockResponse(String type, String status) {
        return new AccountResponseDTO(
            UUID.randomUUID(), type, "Test", new BigDecimal("1000.00"),
            LocalDate.now(), null, status, null, null, null, null, null, null, null
        );
    }
}
