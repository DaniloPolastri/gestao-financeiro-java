package com.findash.mapper;

import com.findash.dto.AccountResponseDTO;
import com.findash.entity.Account;
import com.findash.entity.Category;
import com.findash.entity.Client;
import com.findash.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    default AccountResponseDTO toResponse(Account account, Category category, Supplier supplier, Client client) {
        return new AccountResponseDTO(
            account.getId(),
            account.getType().name(),
            account.getDescription(),
            account.getAmount(),
            account.getDueDate(),
            account.getPaymentDate(),
            account.getStatus().name(),
            category != null ? new AccountResponseDTO.AccountCategoryDTO(category.getId(), category.getName()) : null,
            supplier != null ? new AccountResponseDTO.AccountSupplierDTO(supplier.getId(), supplier.getName()) : null,
            client != null ? new AccountResponseDTO.AccountClientDTO(client.getId(), client.getName()) : null,
            account.getRecurrenceId(),
            account.getNotes(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
