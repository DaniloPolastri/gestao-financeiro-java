package com.findash.service;

import com.findash.dto.*;
import com.findash.entity.AccountStatus;
import com.findash.entity.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponseDTO create(UUID companyId, CreateAccountRequestDTO request);
    AccountResponseDTO getById(UUID companyId, UUID accountId);
    AccountResponseDTO update(UUID companyId, UUID accountId, UpdateAccountRequestDTO request);
    AccountResponseDTO pay(UUID companyId, UUID accountId, PayAccountRequestDTO request);
    void delete(UUID companyId, UUID accountId);
    Page<AccountResponseDTO> list(UUID companyId, AccountType type, List<AccountStatus> statuses,
                                   UUID categoryId, UUID supplierId, UUID clientId,
                                   LocalDate dueDateFrom, LocalDate dueDateTo, Pageable pageable);

    List<AccountResponseDTO> batchPay(UUID companyId, BatchPayRequestDTO request);
}
