package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailUpdateRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailResponse;
import com.pixelMind.materialGrid.service.PersonVehicleDetailImportService;
import com.pixelMind.materialGrid.service.PersonVehicleDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Person Vehicle Details", description = "Person-to-Vehicle assignment records - manual CRUD plus Excel bulk upload")
@RestController
@RequestMapping("/api/v1/person-vehicle-details")
@RequiredArgsConstructor
public class PersonVehicleDetailController {

    private final PersonVehicleDetailService personVehicleDetailService;
    private final PersonVehicleDetailImportService personVehicleDetailImportService;

    @Operation(summary = "Create a person vehicle detail")
    @PostMapping
    public ResponseEntity<ApiResponse<PersonVehicleDetailResponse>> create(
            @Valid @RequestBody PersonVehicleDetailCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Person vehicle detail created successfully",
                        personVehicleDetailService.createPersonVehicleDetail(request)));
    }

    @Operation(summary = "Bulk-upload person vehicle details from an Excel file (Date | Person Code | Vehicle Number). All-or-nothing; rejects a filename+type already uploaded before, and any (person, vehicle, date) already on record.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkUploadResponse>> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResponse result = personVehicleDetailImportService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    @Operation(summary = "Search person vehicle details (paginated; filter by personId, vehicleId, date, startDate/endDate, createdDate, or fileHistoryId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PersonVehicleDetailResponse>>> search(
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdDate,
            @RequestParam(required = false) Long fileHistoryId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<PersonVehicleDetailResponse> page = new PageResponse<>(
                personVehicleDetailService.search(personId, vehicleId, date, startDate, endDate, createdDate, fileHistoryId, pageable));
        return ResponseEntity.ok(ApiResponse.success("Person vehicle details retrieved successfully", page));
    }

    @Operation(summary = "Get a person vehicle detail by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonVehicleDetailResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Person vehicle detail retrieved successfully",
                personVehicleDetailService.getPersonVehicleDetail(id)));
    }

    @Operation(summary = "Update a person vehicle detail")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonVehicleDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PersonVehicleDetailUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Person vehicle detail updated successfully",
                personVehicleDetailService.updatePersonVehicleDetail(id, request)));
    }

    @Operation(summary = "Delete (soft-delete) a person vehicle detail")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        personVehicleDetailService.deletePersonVehicleDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Person vehicle detail deleted successfully", null));
    }
}