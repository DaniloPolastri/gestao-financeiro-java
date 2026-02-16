package com.findash.service;

import com.findash.dto.supplier.CreateSupplierRequestDTO;
import com.findash.dto.supplier.SupplierResponseDTO;
import com.findash.dto.supplier.UpdateSupplierRequestDTO;
import java.util.List;
import java.util.UUID;

public interface SupplierService {

    SupplierResponseDTO create(UUID companyId, CreateSupplierRequestDTO request);

    List<SupplierResponseDTO> list(UUID companyId);

    SupplierResponseDTO getById(UUID companyId, UUID supplierId);

    SupplierResponseDTO update(UUID companyId, UUID supplierId, UpdateSupplierRequestDTO request);

    void delete(UUID companyId, UUID supplierId);
}
