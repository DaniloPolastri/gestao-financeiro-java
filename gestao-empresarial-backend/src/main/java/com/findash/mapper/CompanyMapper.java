package com.findash.mapper;

import com.findash.dto.CompanyMemberResponseDTO;
import com.findash.dto.CompanyResponseDTO;
import com.findash.entity.Company;
import com.findash.entity.CompanyMember;
import com.findash.util.CnpjValidator;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    default CompanyResponseDTO toCompanyResponse(Company company, String role, String ownerName) {
        return new CompanyResponseDTO(
            company.getId(),
            company.getName(),
            CnpjValidator.format(company.getCnpj()),
            company.getSegment(),
            company.getOwnerId(),
            ownerName,
            role,
            company.isActive()
        );
    }

    default CompanyMemberResponseDTO toMemberResponse(CompanyMember member, String name,
                                                       String email, String role) {
        return new CompanyMemberResponseDTO(
            member.getUserId(),
            name,
            email,
            role,
            member.getStatus().name(),
            member.getJoinedAt()
        );
    }
}
