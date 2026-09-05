package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PersonVehicleDetailImportService {

    BulkUploadResponse importFromExcel(MultipartFile file);
}