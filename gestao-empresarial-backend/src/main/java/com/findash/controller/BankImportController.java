package com.findash.controller;

import com.findash.dto.bankimport.*;
import com.findash.security.CompanyContextHolder;
import com.findash.security.UserContext;
import com.findash.service.BankImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/imports")
public class BankImportController {

    private final BankImportService bankImportService;

    public BankImportController(BankImportService bankImportService) {
        this.bankImportService = bankImportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<BankImportResponseDTO> upload(
            @RequestParam("file") MultipartFile file) {
        UUID companyId = CompanyContextHolder.get();
        UUID userId = resolveUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bankImportService.upload(companyId, userId, file));
    }

    @GetMapping
    public ResponseEntity<List<BankImportSummaryDTO>> list() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(bankImportService.list(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankImportResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(bankImportService.getById(companyId, id));
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ResponseEntity<BankImportItemResponseDTO> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody UpdateImportItemRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(bankImportService.updateItem(companyId, id, itemId, request));
    }

    @PatchMapping("/{id}/items/batch")
    public ResponseEntity<List<BankImportItemResponseDTO>> updateItemsBatch(
            @PathVariable UUID id,
            @RequestBody BatchUpdateImportItemsRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(bankImportService.updateItemsBatch(companyId, id, request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        bankImportService.confirm(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        bankImportService.cancel(companyId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserContext userCtx) {
            return userCtx.userId();
        }
        return null;
    }
}
