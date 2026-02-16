package com.findash.controller;

import com.findash.dto.*;
import com.findash.security.UserContext;
import com.findash.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(
            @Valid @RequestBody CreateCompanyRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createCompany(request, user.userId()));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> listCompanies(
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.listUserCompanies(user.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> getCompany(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.getCompany(id, user.userId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.updateCompany(id, request, user.userId()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CompanyMemberResponseDTO>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.listMembers(id, user.userId()));
    }

    @PostMapping("/{id}/members/invite")
    public ResponseEntity<CompanyMemberResponseDTO> inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.inviteMember(id, request, user.userId()));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        service.updateMemberRole(id, memberId, request, user.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserContext user) {
        service.removeMember(id, memberId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
