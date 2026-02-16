package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ForbiddenOperationException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CompanyMapper;
import com.findash.repository.*;
import com.findash.service.CompanyService;
import com.findash.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              CompanyMemberRepository companyMemberRepository,
                              UserRoleRepository userRoleRepository,
                              UserRepository userRepository,
                              CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.companyMapper = companyMapper;
    }

    @Override
    @Transactional
    public CompanyResponseDTO createCompany(CreateCompanyRequestDTO request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String normalizedCnpj = null;
        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            normalizedCnpj = CnpjValidator.normalize(request.cnpj());
            if (!CnpjValidator.isValid(normalizedCnpj)) {
                throw new BusinessRuleException("CNPJ invalido");
            }
            if (companyRepository.existsByCnpj(normalizedCnpj)) {
                throw new DuplicateResourceException("CNPJ ja cadastrado");
            }
        }

        Company company = new Company(request.name(), normalizedCnpj, request.segment(), userId);
        company = companyRepository.save(company);

        CompanyMember member = new CompanyMember();
        member.setCompanyId(company.getId());
        member.setUserId(userId);
        member.setInvitedEmail(user.getEmail());
        member.setStatus(MemberStatus.ACTIVE);
        member.setInvitedAt(Instant.now());
        member.setJoinedAt(Instant.now());
        companyMemberRepository.save(member);

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setCompanyId(company.getId());
        userRole.setRole(Role.ADMIN);
        userRoleRepository.save(userRole);

        return companyMapper.toCompanyResponse(company, Role.ADMIN.name(), user.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> listUserCompanies(UUID userId) {
        List<Company> companies = companyRepository.findByMemberUserIdAndStatus(userId, MemberStatus.ACTIVE);

        return companies.stream().map(company -> {
            UserRole role = userRoleRepository.findByUserIdAndCompanyId(userId, company.getId())
                    .orElse(null);
            String roleName = role != null ? role.getRole().name() : null;

            String ownerName = userRepository.findById(company.getOwnerId())
                    .map(User::getName)
                    .orElse(null);

            return companyMapper.toCompanyResponse(company, roleName, ownerName);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompany(UUID companyId, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        UserRole role = requireMembership(userId, companyId);

        String ownerName = userRepository.findById(company.getOwnerId())
                .map(User::getName)
                .orElse(null);

        return companyMapper.toCompanyResponse(company, role.getRole().name(), ownerName);
    }

    @Override
    @Transactional
    public CompanyResponseDTO updateCompany(UUID companyId, UpdateCompanyRequestDTO request, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            String normalizedCnpj = CnpjValidator.normalize(request.cnpj());
            if (!CnpjValidator.isValid(normalizedCnpj)) {
                throw new BusinessRuleException("CNPJ invalido");
            }
            // Allow same company to keep its CNPJ
            if (!normalizedCnpj.equals(company.getCnpj()) && companyRepository.existsByCnpj(normalizedCnpj)) {
                throw new DuplicateResourceException("CNPJ ja cadastrado");
            }
            company.setCnpj(normalizedCnpj);
        } else {
            company.setCnpj(null);
        }

        company.setName(request.name());
        company.setSegment(request.segment());
        company = companyRepository.save(company);

        String ownerName = userRepository.findById(company.getOwnerId())
                .map(User::getName)
                .orElse(null);

        return companyMapper.toCompanyResponse(company, Role.ADMIN.name(), ownerName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyMemberResponseDTO> listMembers(UUID companyId, UUID userId) {
        findCompanyOrThrow(companyId);
        UserRole callerRole = requireMembership(userId, companyId);

        if (callerRole.getRole() == Role.VIEWER) {
            throw new ForbiddenOperationException("Viewer nao pode listar membros");
        }

        List<CompanyMember> members = companyMemberRepository
                .findByCompanyIdAndStatusNot(companyId, MemberStatus.REMOVED);

        return members.stream().map(member -> {
            String name = null;
            String email = member.getInvitedEmail();
            String roleName = null;

            if (member.getUserId() != null) {
                Optional<User> memberUser = userRepository.findById(member.getUserId());
                if (memberUser.isPresent()) {
                    name = memberUser.get().getName();
                    email = memberUser.get().getEmail();
                }

                UserRole memberRole = userRoleRepository
                        .findByUserIdAndCompanyId(member.getUserId(), companyId)
                        .orElse(null);
                if (memberRole != null) {
                    roleName = memberRole.getRole().name();
                }
            }

            return companyMapper.toMemberResponse(member, name, email, roleName);
        }).toList();
    }

    @Override
    @Transactional
    public CompanyMemberResponseDTO inviteMember(UUID companyId, InviteMemberRequestDTO request, UUID userId) {
        findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (request.role() == Role.ADMIN) {
            throw new BusinessRuleException("Nao e possivel convidar com role ADMIN");
        }

        boolean alreadyExists = companyMemberRepository
                .existsByCompanyIdAndInvitedEmailAndStatusIn(
                        companyId, request.email(), List.of(MemberStatus.ACTIVE, MemberStatus.INVITED));
        if (alreadyExists) {
            throw new DuplicateResourceException("Membro ja convidado ou ativo nesta empresa");
        }

        Optional<User> existingUser = userRepository.findByEmail(request.email());

        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setInvitedEmail(request.email());
        member.setInvitedAt(Instant.now());

        String memberName = null;

        if (existingUser.isPresent()) {
            User invitee = existingUser.get();
            member.setUserId(invitee.getId());
            member.setStatus(MemberStatus.ACTIVE);
            member.setJoinedAt(Instant.now());
            memberName = invitee.getName();

            UserRole userRole = new UserRole();
            userRole.setUserId(invitee.getId());
            userRole.setCompanyId(companyId);
            userRole.setRole(request.role());
            userRoleRepository.save(userRole);
        } else {
            member.setStatus(MemberStatus.INVITED);
        }

        member = companyMemberRepository.save(member);

        return companyMapper.toMemberResponse(member, memberName, request.email(), request.role().name());
    }

    @Override
    @Transactional
    public void updateMemberRole(UUID companyId, UUID memberId, UpdateMemberRoleRequestDTO request, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (company.getOwnerId().equals(memberId)) {
            throw new ForbiddenOperationException("Nao e possivel alterar role do proprietario");
        }

        UserRole memberRole = userRoleRepository.findByUserIdAndCompanyId(memberId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        // If target is currently ADMIN and we're demoting, check if they're the last ADMIN
        if (memberRole.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            long adminCount = userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessRuleException("Nao e possivel remover o ultimo ADMIN da empresa");
            }
        }

        memberRole.setRole(request.role());
        userRoleRepository.save(memberRole);
    }

    @Override
    @Transactional
    public void removeMember(UUID companyId, UUID memberId, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        CompanyMember targetMember = companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyMember", memberId));

        if (targetMember.getUserId() != null && targetMember.getUserId().equals(company.getOwnerId())) {
            throw new ForbiddenOperationException("Nao e possivel remover o proprietario da empresa");
        }

        // Check if target is last ADMIN before removing
        if (targetMember.getUserId() != null) {
            Optional<UserRole> targetRole = userRoleRepository
                    .findByUserIdAndCompanyId(targetMember.getUserId(), companyId);
            if (targetRole.isPresent() && targetRole.get().getRole() == Role.ADMIN) {
                long adminCount = userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN);
                if (adminCount <= 1) {
                    throw new BusinessRuleException("Nao e possivel remover o ultimo ADMIN da empresa");
                }
            }
        }

        targetMember.setStatus(MemberStatus.REMOVED);
        companyMemberRepository.save(targetMember);

        if (targetMember.getUserId() != null) {
            userRoleRepository.deleteByUserIdAndCompanyId(targetMember.getUserId(), companyId);
        }
    }

    // ===================== Helper Methods =====================

    private Company findCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }

    private UserRole requireMembership(UUID userId, UUID companyId) {
        return userRoleRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Usuario nao e membro desta empresa"));
    }

    private UserRole requireAdmin(UUID userId, UUID companyId) {
        UserRole role = requireMembership(userId, companyId);
        if (role.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Apenas administradores podem realizar esta operacao");
        }
        return role;
    }
}
