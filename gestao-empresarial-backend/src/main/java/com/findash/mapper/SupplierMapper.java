package com.findash.mapper;

import com.findash.dto.supplier.SupplierResponseDTO;
import com.findash.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    default SupplierResponseDTO toResponse(Supplier supplier) {
        return new SupplierResponseDTO(
            supplier.getId(),
            supplier.getName(),
            supplier.getDocument(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.isActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}
