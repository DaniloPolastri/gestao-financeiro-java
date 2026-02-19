package com.findash.service;

import com.findash.dto.bankimport.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface BankImportService {
    BankImportResponseDTO upload(UUID companyId, UUID userId, MultipartFile file);
    List<BankImportSummaryDTO> list(UUID companyId);
    BankImportResponseDTO getById(UUID companyId, UUID importId);
    BankImportItemResponseDTO updateItem(UUID companyId, UUID importId, UUID itemId, UpdateImportItemRequestDTO request);
    List<BankImportItemResponseDTO> updateItemsBatch(UUID companyId, UUID importId, BatchUpdateImportItemsRequestDTO request);
    void confirm(UUID companyId, UUID importId);
    void cancel(UUID companyId, UUID importId);
}
