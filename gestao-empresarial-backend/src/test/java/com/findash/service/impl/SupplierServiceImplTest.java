package com.findash.service.impl;

import com.findash.dto.supplier.CreateSupplierRequestDTO;
import com.findash.dto.supplier.SupplierResponseDTO;
import com.findash.dto.supplier.UpdateSupplierRequestDTO;
import com.findash.entity.Supplier;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.SupplierMapper;
import com.findash.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    private SupplierServiceImpl supplierService;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierServiceImpl(supplierRepository, supplierMapper);
        companyId = UUID.randomUUID();
    }

    // --- CREATE ---

    @Test
    void createSupplier_success() {
        var request = new CreateSupplierRequestDTO("Fornecedor A", "11222333000181", "a@test.com", "11999990000");
        var supplier = new Supplier(companyId, "Fornecedor A");
        supplier.setId(UUID.randomUUID());
        var response = new SupplierResponseDTO(supplier.getId(), "Fornecedor A", "11222333000181", "a@test.com", "11999990000", true, null, null);

        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Fornecedor A")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(supplierMapper.toResponse(any(Supplier.class))).thenReturn(response);

        SupplierResponseDTO result = supplierService.create(companyId, request);

        assertNotNull(result);
        assertEquals("Fornecedor A", result.name());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_duplicateName_throws() {
        var request = new CreateSupplierRequestDTO("Fornecedor A", null, null, null);
        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Fornecedor A")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> supplierService.create(companyId, request));
        verify(supplierRepository, never()).save(any());
    }

    // --- LIST ---

    @Test
    void listSuppliers_returnsAll() {
        var s1 = new Supplier(companyId, "A");
        var s2 = new Supplier(companyId, "B");
        when(supplierRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of(s1, s2));
        when(supplierMapper.toResponse(s1)).thenReturn(new SupplierResponseDTO(UUID.randomUUID(), "A", null, null, null, true, null, null));
        when(supplierMapper.toResponse(s2)).thenReturn(new SupplierResponseDTO(UUID.randomUUID(), "B", null, null, null, true, null, null));

        List<SupplierResponseDTO> result = supplierService.list(companyId);

        assertEquals(2, result.size());
    }

    // --- GET BY ID ---

    @Test
    void getSupplier_found() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "A");
        supplier.setId(supplierId);
        var response = new SupplierResponseDTO(supplierId, "A", null, null, null, true, null, null);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        SupplierResponseDTO result = supplierService.getById(companyId, supplierId);

        assertEquals("A", result.name());
    }

    @Test
    void getSupplier_notFound_throws() {
        UUID supplierId = UUID.randomUUID();
        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.getById(companyId, supplierId));
    }

    // --- UPDATE ---

    @Test
    void updateSupplier_success() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "Old Name");
        supplier.setId(supplierId);
        var request = new UpdateSupplierRequestDTO("New Name", null, null, null);
        var response = new SupplierResponseDTO(supplierId, "New Name", null, null, null, true, null, null);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "New Name")).thenReturn(false);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        SupplierResponseDTO result = supplierService.update(companyId, supplierId, request);

        assertEquals("New Name", result.name());
    }

    // --- DELETE (soft) ---

    @Test
    void deleteSupplier_setsInactive() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "A");
        supplier.setId(supplierId);
        supplier.setActive(true);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));

        supplierService.delete(companyId, supplierId);

        assertFalse(supplier.isActive());
        verify(supplierRepository).save(supplier);
    }
}
