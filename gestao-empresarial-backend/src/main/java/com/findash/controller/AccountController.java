package com.findash.controller;

import com.findash.dto.*;
import com.findash.entity.AccountStatus;
import com.findash.entity.AccountType;
import com.findash.security.CompanyContextHolder;
import com.findash.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> list(
            @RequestParam AccountType type,
            @RequestParam(required = false) List<AccountStatus> status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(accountService.list(companyId, type, status, categoryId, supplierId, clientId, dueDateFrom, dueDateTo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(accountService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<AccountResponseDTO> create(@Valid @RequestBody CreateAccountRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(accountService.update(companyId, id, request));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<AccountResponseDTO> pay(
            @PathVariable UUID id,
            @Valid @RequestBody PayAccountRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(accountService.pay(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        accountService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
