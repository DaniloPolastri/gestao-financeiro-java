package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ForbiddenOperationException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CompanyMapper;
import com.findash.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyMemberRepository companyMemberRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyMapper companyMapper;

    private CompanyServiceImpl companyService;

    private UUID userId;
    private UUID companyId;
    private UUID memberId;
    private User user;
    private Company company;

    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(
                companyRepository, companyMemberRepository,
                userRoleRepository, userRepository, companyMapper);

        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        user = new User("Joao Silva", "joao@email.com", "password");
        user.setId(userId);

        company = new Company("Empresa Teste", "11222333000181", "Tecnologia", userId);
        company.setId(companyId);
    }

    // ===================== CRUD Tests =====================

    @Test
    void createCompany_success() {
        var request = new CreateCompanyRequestDTO("Empresa Teste", "11.222.333/0001-81", "Tecnologia");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(companyId);
            return c;
        });
        when(companyMemberRepository.save(any(CompanyMember.class))).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(i -> i.getArgument(0));
        when(companyMapper.toCompanyResponse(any(Company.class), eq("ADMIN"), eq("Joao Silva")))
                .thenReturn(new CompanyResponseDTO(companyId, "Empresa Teste", "11.222.333/0001-81",
                        "Tecnologia", userId, "Joao Silva", "ADMIN", true));

        CompanyResponseDTO result = companyService.createCompany(request, userId);

        assertNotNull(result);
        assertEquals("Empresa Teste", result.name());
        assertEquals("ADMIN", result.role());

        verify(companyRepository).save(any(Company.class));

        ArgumentCaptor<CompanyMember> memberCaptor = ArgumentCaptor.forClass(CompanyMember.class);
        verify(companyMemberRepository).save(memberCaptor.capture());
        CompanyMember savedMember = memberCaptor.getValue();
        assertEquals(MemberStatus.ACTIVE, savedMember.getStatus());
        assertEquals(userId, savedMember.getUserId());
        assertEquals("joao@email.com", savedMember.getInvitedEmail());

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        UserRole savedRole = roleCaptor.getValue();
        assertEquals(Role.ADMIN, savedRole.getRole());
        assertEquals(userId, savedRole.getUserId());
    }

    @Test
    void createCompany_withInvalidCnpj_throwsException() {
        var request = new CreateCompanyRequestDTO("Empresa", "12345678901234", "Tech");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BusinessRuleException.class,
                () -> companyService.createCompany(request, userId));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void createCompany_withDuplicateCnpj_throwsException() {
        var request = new CreateCompanyRequestDTO("Empresa", "11.222.333/0001-81", "Tech");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> companyService.createCompany(request, userId));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void listUserCompanies_returnsCompaniesWithRoles() {
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setCompanyId(companyId);
        role.setRole(Role.ADMIN);

        when(companyRepository.findByMemberUserIdAndStatus(userId, MemberStatus.ACTIVE))
                .thenReturn(List.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(role));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyMapper.toCompanyResponse(company, "ADMIN", "Joao Silva"))
                .thenReturn(new CompanyResponseDTO(companyId, "Empresa Teste", "11.222.333/0001-81",
                        "Tecnologia", userId, "Joao Silva", "ADMIN", true));

        List<CompanyResponseDTO> result = companyService.listUserCompanies(userId);

        assertEquals(1, result.size());
        assertEquals("Empresa Teste", result.get(0).name());
        assertEquals("ADMIN", result.get(0).role());
    }

    @Test
    void getCompany_success() {
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setCompanyId(companyId);
        role.setRole(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(role));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyMapper.toCompanyResponse(company, "EDITOR", "Joao Silva"))
                .thenReturn(new CompanyResponseDTO(companyId, "Empresa Teste", "11.222.333/0001-81",
                        "Tecnologia", userId, "Joao Silva", "EDITOR", true));

        CompanyResponseDTO result = companyService.getCompany(companyId, userId);

        assertNotNull(result);
        assertEquals("EDITOR", result.role());
    }

    @Test
    void getCompany_notMember_throwsForbidden() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class,
                () -> companyService.getCompany(companyId, userId));
    }

    @Test
    void updateCompany_asAdmin_success() {
        var request = new UpdateCompanyRequestDTO("Novo Nome", "11.222.333/0001-81", "Fintech");

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyMapper.toCompanyResponse(any(Company.class), eq("ADMIN"), eq("Joao Silva")))
                .thenReturn(new CompanyResponseDTO(companyId, "Novo Nome", "11.222.333/0001-81",
                        "Fintech", userId, "Joao Silva", "ADMIN", true));

        CompanyResponseDTO result = companyService.updateCompany(companyId, request, userId);

        assertNotNull(result);
        assertEquals("Novo Nome", result.name());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void updateCompany_asEditor_throwsForbidden() {
        var request = new UpdateCompanyRequestDTO("Novo Nome", null, "Fintech");

        UserRole editorRole = new UserRole();
        editorRole.setUserId(userId);
        editorRole.setCompanyId(companyId);
        editorRole.setRole(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(editorRole));

        assertThrows(ForbiddenOperationException.class,
                () -> companyService.updateCompany(companyId, request, userId));
        verify(companyRepository, never()).save(any());
    }

    // ===================== Member Management Tests =====================

    @Test
    void inviteMember_existingUser_activatesImmediately() {
        String inviteeEmail = "maria@email.com";
        UUID inviteeUserId = UUID.randomUUID();
        User invitee = new User("Maria Santos", inviteeEmail, "password");
        invitee.setId(inviteeUserId);

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        var request = new InviteMemberRequestDTO(inviteeEmail, Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq(inviteeEmail), anyList())).thenReturn(false);
        when(userRepository.findByEmail(inviteeEmail)).thenReturn(Optional.of(invitee));
        when(companyMemberRepository.save(any(CompanyMember.class))).thenAnswer(i -> {
            CompanyMember m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(i -> i.getArgument(0));
        when(companyMapper.toMemberResponse(any(CompanyMember.class), eq("Maria Santos"),
                eq(inviteeEmail), eq("EDITOR")))
                .thenReturn(new CompanyMemberResponseDTO(inviteeUserId, "Maria Santos",
                        inviteeEmail, "EDITOR", "ACTIVE", Instant.now()));

        CompanyMemberResponseDTO result = companyService.inviteMember(companyId, request, userId);

        assertNotNull(result);
        assertEquals("ACTIVE", result.status());

        ArgumentCaptor<CompanyMember> memberCaptor = ArgumentCaptor.forClass(CompanyMember.class);
        verify(companyMemberRepository).save(memberCaptor.capture());
        assertEquals(MemberStatus.ACTIVE, memberCaptor.getValue().getStatus());
        assertEquals(inviteeUserId, memberCaptor.getValue().getUserId());

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void inviteMember_nonExistingUser_createsInvitedStatus() {
        String inviteeEmail = "novousuario@email.com";

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        var request = new InviteMemberRequestDTO(inviteeEmail, Role.VIEWER);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq(inviteeEmail), anyList())).thenReturn(false);
        when(userRepository.findByEmail(inviteeEmail)).thenReturn(Optional.empty());
        when(companyMemberRepository.save(any(CompanyMember.class))).thenAnswer(i -> {
            CompanyMember m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(companyMapper.toMemberResponse(any(CompanyMember.class), isNull(),
                eq(inviteeEmail), eq("VIEWER")))
                .thenReturn(new CompanyMemberResponseDTO(null, null,
                        inviteeEmail, "VIEWER", "INVITED", null));

        CompanyMemberResponseDTO result = companyService.inviteMember(companyId, request, userId);

        assertNotNull(result);
        assertEquals("INVITED", result.status());

        ArgumentCaptor<CompanyMember> memberCaptor = ArgumentCaptor.forClass(CompanyMember.class);
        verify(companyMemberRepository).save(memberCaptor.capture());
        assertEquals(MemberStatus.INVITED, memberCaptor.getValue().getStatus());
        assertNull(memberCaptor.getValue().getUserId());

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void inviteMember_duplicateInvite_throwsException() {
        String inviteeEmail = "maria@email.com";

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        var request = new InviteMemberRequestDTO(inviteeEmail, Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq(inviteeEmail), anyList())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> companyService.inviteMember(companyId, request, userId));
        verify(companyMemberRepository, never()).save(any());
    }

    @Test
    void inviteMember_asAdmin_throwsException() {
        String inviteeEmail = "maria@email.com";

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        var request = new InviteMemberRequestDTO(inviteeEmail, Role.ADMIN);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));

        assertThrows(BusinessRuleException.class,
                () -> companyService.inviteMember(companyId, request, userId));
        verify(companyMemberRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_success() {
        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        UserRole targetRole = new UserRole();
        targetRole.setUserId(memberId);
        targetRole.setCompanyId(companyId);
        targetRole.setRole(Role.EDITOR);

        var request = new UpdateMemberRoleRequestDTO(Role.VIEWER);

        // Company has a different owner (not memberId)
        Company comp = new Company("Test", null, null, UUID.randomUUID());
        comp.setId(companyId);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(comp));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(userRoleRepository.findByUserIdAndCompanyId(memberId, companyId))
                .thenReturn(Optional.of(targetRole));

        companyService.updateMemberRole(companyId, memberId, request, userId);

        verify(userRoleRepository).save(targetRole);
        assertEquals(Role.VIEWER, targetRole.getRole());
    }

    @Test
    void updateMemberRole_owner_throwsForbidden() {
        UUID ownerUserId = memberId; // memberId IS the owner

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        // Company owner IS the target (memberId == ownerId)
        Company comp = new Company("Test", null, null, ownerUserId);
        comp.setId(companyId);

        var request = new UpdateMemberRoleRequestDTO(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(comp));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));

        assertThrows(ForbiddenOperationException.class,
                () -> companyService.updateMemberRole(companyId, memberId, request, userId));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_lastAdmin_throwsException() {
        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        UserRole targetRole = new UserRole();
        targetRole.setUserId(memberId);
        targetRole.setCompanyId(companyId);
        targetRole.setRole(Role.ADMIN);

        // Owner is someone else (not memberId)
        Company comp = new Company("Test", null, null, UUID.randomUUID());
        comp.setId(companyId);

        var request = new UpdateMemberRoleRequestDTO(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(comp));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(userRoleRepository.findByUserIdAndCompanyId(memberId, companyId))
                .thenReturn(Optional.of(targetRole));
        when(userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN)).thenReturn(1L);

        assertThrows(BusinessRuleException.class,
                () -> companyService.updateMemberRole(companyId, memberId, request, userId));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void removeMember_success() {
        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        CompanyMember targetMember = new CompanyMember();
        targetMember.setCompanyId(companyId);
        targetMember.setUserId(memberId);
        targetMember.setInvitedEmail("target@email.com");
        targetMember.setStatus(MemberStatus.ACTIVE);

        // Owner is someone else (not memberId)
        Company comp = new Company("Test", null, null, UUID.randomUUID());
        comp.setId(companyId);

        UserRole targetRole = new UserRole();
        targetRole.setUserId(memberId);
        targetRole.setCompanyId(companyId);
        targetRole.setRole(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(comp));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId))
                .thenReturn(Optional.of(targetMember));
        when(userRoleRepository.findByUserIdAndCompanyId(memberId, companyId))
                .thenReturn(Optional.of(targetRole));

        companyService.removeMember(companyId, memberId, userId);

        assertEquals(MemberStatus.REMOVED, targetMember.getStatus());
        verify(companyMemberRepository).save(targetMember);
        verify(userRoleRepository).deleteByUserIdAndCompanyId(memberId, companyId);
    }

    @Test
    void removeMember_owner_throwsForbidden() {
        UUID ownerUserId = memberId; // memberId IS the owner

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        CompanyMember ownerMember = new CompanyMember();
        ownerMember.setCompanyId(companyId);
        ownerMember.setUserId(ownerUserId);
        ownerMember.setStatus(MemberStatus.ACTIVE);

        // Company owner IS the target (memberId == ownerId)
        Company comp = new Company("Test", null, null, ownerUserId);
        comp.setId(companyId);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(comp));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId))
                .thenReturn(Optional.of(ownerMember));

        assertThrows(ForbiddenOperationException.class,
                () -> companyService.removeMember(companyId, memberId, userId));
        verify(companyMemberRepository, never()).save(any());
    }

    @Test
    void listMembers_success() {
        UUID member2UserId = UUID.randomUUID();
        User member2User = new User("Maria Santos", "maria@email.com", "password");
        member2User.setId(member2UserId);

        UserRole adminRole = new UserRole();
        adminRole.setUserId(userId);
        adminRole.setCompanyId(companyId);
        adminRole.setRole(Role.ADMIN);

        CompanyMember member1 = new CompanyMember();
        member1.setCompanyId(companyId);
        member1.setUserId(userId);
        member1.setInvitedEmail("joao@email.com");
        member1.setStatus(MemberStatus.ACTIVE);
        member1.setJoinedAt(Instant.now());

        CompanyMember member2 = new CompanyMember();
        member2.setCompanyId(companyId);
        member2.setUserId(member2UserId);
        member2.setInvitedEmail("maria@email.com");
        member2.setStatus(MemberStatus.ACTIVE);
        member2.setJoinedAt(Instant.now());

        UserRole member2Role = new UserRole();
        member2Role.setUserId(member2UserId);
        member2Role.setCompanyId(companyId);
        member2Role.setRole(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.findByCompanyIdAndStatusNot(companyId, MemberStatus.REMOVED))
                .thenReturn(List.of(member1, member2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(member2UserId)).thenReturn(Optional.of(member2User));
        when(userRoleRepository.findByUserIdAndCompanyId(member2UserId, companyId))
                .thenReturn(Optional.of(member2Role));
        when(companyMapper.toMemberResponse(eq(member1), eq("Joao Silva"), eq("joao@email.com"), eq("ADMIN")))
                .thenReturn(new CompanyMemberResponseDTO(userId, "Joao Silva", "joao@email.com",
                        "ADMIN", "ACTIVE", member1.getJoinedAt()));
        when(companyMapper.toMemberResponse(eq(member2), eq("Maria Santos"), eq("maria@email.com"), eq("EDITOR")))
                .thenReturn(new CompanyMemberResponseDTO(member2UserId, "Maria Santos", "maria@email.com",
                        "EDITOR", "ACTIVE", member2.getJoinedAt()));

        List<CompanyMemberResponseDTO> result = companyService.listMembers(companyId, userId);

        assertEquals(2, result.size());
    }
}
