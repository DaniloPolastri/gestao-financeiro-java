package com.findash.service.impl;

import com.findash.dto.supplier.CreateSupplierRequestDTO;
import com.findash.dto.supplier.SupplierResponseDTO;
import com.findash.dto.supplier.UpdateSupplierRequestDTO;
import com.findash.entity.Supplier;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.SupplierMapper;
import com.findash.repository.SupplierRepository;
import com.findash.service.SupplierService;
import com.findash.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public SupplierResponseDTO create(UUID companyId, CreateSupplierRequestDTO request) {
        if (supplierRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este nome");
        }

        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        if (normalizedDoc != null && !normalizedDoc.isBlank()
                && supplierRepository.existsByCompanyIdAndDocumentAndActiveTrue(companyId, normalizedDoc)) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este documento");
        }

        Supplier supplier = new Supplier(companyId, request.name().trim());
        supplier.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());

        supplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> list(UUID companyId) {
        return supplierRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getById(UUID companyId, UUID supplierId) {
        Supplier supplier = findOrThrow(companyId, supplierId);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public SupplierResponseDTO update(UUID companyId, UUID supplierId, UpdateSupplierRequestDTO request) {
        Supplier supplier = findOrThrow(companyId, supplierId);

        if (!supplier.getName().equalsIgnoreCase(request.name())
                && supplierRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este nome");
        }

        supplier.setName(request.name().trim());
        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        supplier.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());

        supplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public void delete(UUID companyId, UUID supplierId) {
        Supplier supplier = findOrThrow(companyId, supplierId);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private Supplier findOrThrow(UUID companyId, UUID supplierId) {
        return supplierRepository.findByIdAndCompanyId(supplierId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", supplierId));
    }
}
