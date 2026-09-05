package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.VehicleCreateRequest;
import com.pixelMind.materialGrid.dto.request.VehicleUpdateRequest;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.dto.response.VehicleResponse;
import com.pixelMind.materialGrid.entity.DailyRoute;
import com.pixelMind.materialGrid.entity.Route;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.VehicleMapper;
import com.pixelMind.materialGrid.repository.DailyRouteRepository;
import com.pixelMind.materialGrid.repository.PersonVehicleDetailRepository;
import com.pixelMind.materialGrid.repository.VehicleExpenseRepository;
import com.pixelMind.materialGrid.repository.VehicleLicenseRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.VehicleService;
import com.pixelMind.materialGrid.util.ExcelUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final Pattern VEHICLE_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9-]{4,20}$");

    private final VehicleRepository vehicleRepository;
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleLicenseRepository vehicleLicenseRepository;
    private final DailyRouteRepository dailyRouteRepository;
    private final PersonVehicleDetailRepository personVehicleDetailRepository;
    private final VehicleMapper vehicleMapper;

    private record RawVehicleRow(int rowNumber, String vehicleNumber, BigDecimal capacity, boolean hasFormatError) {
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {

        log.info("VehicleServiceImpl.createVehicle => accessed");

        String vehicleNumber = request.getVehicleNumber().toUpperCase();

        if (vehicleRepository.existsByVehicleNumberAndDeletedFalse(vehicleNumber)) {
            throw new DuplicateResourceException(
                    "Vehicle number already exists: " + vehicleNumber,
                    ErrorCodeConstants.DUPLICATE_VEHICLE_NUMBER);
        }

        String actor = SecurityUtil.getCurrentUsername();
        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(vehicleNumber)
                .capacity(request.getCapacity())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        // Application-level check above is the fast, friendly-error path;
        // the DB unique constraint on vehicle_number (see
        // V6__create_vehicles_table.sql) is the real backstop against two
        // concurrent requests both passing the check before either commits.
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}, vehicleNumber={}, by={}", saved.getId(), saved.getVehicleNumber(), actor);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        return vehicleMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehicles(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return vehicleRepository.findByVehicleNumberContainingIgnoreCaseAndDeletedFalse(search, pageable)
                    .map(vehicleMapper::toResponse);
        }
        return vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        String actor = SecurityUtil.getCurrentUsername();

        Vehicle vehicle = findOrThrow(id);
        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setModifiedBy(SecurityUtil.getCurrentUsername());

        if (!vehicle.getCapacity().equals(request.getCapacity())) {

            List<DailyRoute> dailyRoutes = dailyRouteRepository.findByVehicle(vehicle.getId());

            dailyRoutes.forEach(dailyRoute -> {

                dailyRoute.setAmount(
                        request.getCapacity()
                                .multiply(dailyRoute.getRoute().getPrice())
                                .multiply(dailyRoute.getRoute().getKm())
                                .setScale(2, RoundingMode.HALF_UP)
                );

            });

            dailyRouteRepository.saveAll(dailyRoutes);

            vehicle.setCapacity(request.getCapacity());

        }

        Vehicle saved = vehicleRepository.save(vehicle);

        List<DailyRoute> dailyRoutes = dailyRouteRepository.findByVehicleIdAndDeletedFalse(id)
                .stream()
                .peek(dailyRoute -> {
                    dailyRoute.setAmount(
                            computeAmount(saved, dailyRoute.getRoute())
                    );
                    dailyRoute.setModifiedBy(actor);
                })
                .toList();

        dailyRouteRepository.saveAll(dailyRoutes);

        log.info("Vehicle updated: id={}, by={}", saved.getId(), vehicle.getModifiedBy());
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findOrThrow(id);

        if (vehicleExpenseRepository.existsByVehicleIdAndDeletedFalse(id)
                || vehicleLicenseRepository.existsByVehicleIdAndDeletedFalse(id)
                || dailyRouteRepository.existsByVehicleIdAndDeletedFalse(id)
                || personVehicleDetailRepository.existsByVehicleIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Cannot delete vehicle with existing expense, license, daily route, or person vehicle detail records. "
                            + "These are historical records and this vehicle must be preserved for referential integrity.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadVehicles(MultipartFile file) {
        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);

            Integer vehicleCol = findHeader(headerIndex, "vehicle number", "vehiclenumber", "vehicle no", "vehicle");
            Integer capacityCol = findHeader(headerIndex, "capacity(cube)", "capacity (cube)", "capacity( cube )", "capacity");

            List<String> missingHeaders = new ArrayList<>();
            if (vehicleCol == null) {
                missingHeaders.add("Vehicle Number");
            }
            if (capacityCol == null) {
                missingHeaders.add("Capacity(cube)");
            }

            if (!missingHeaders.isEmpty()) {
                throw new ExcelValidationException(
                        "Missing required column(s): " + String.join(", ", missingHeaders)
                                + ". Expected headers: Vehicle Number, Capacity(cube)",
                        List.of(error(0, "File", null, "Missing required column(s): " + String.join(", ", missingHeaders))),
                        0);
            }

            List<ExcelValidationError> errors = new ArrayList<>();
            List<RawVehicleRow> rawRows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                int rowNumber = r + 1; // 1-indexed as seen in Excel (header = row 1)

                String rawVehicleNumber = ExcelUtil.readString(row, vehicleCol);
                String vehicleNumber = rawVehicleNumber.trim().toUpperCase();
                boolean vehicleNumberError = false;

                if (vehicleNumber.isBlank()) {
                    errors.add(error(rowNumber, "Vehicle Number", null, "Vehicle number is required"));
                    vehicleNumberError = true;
                } else if (!VEHICLE_NUMBER_PATTERN.matcher(vehicleNumber).matches()) {
                    errors.add(error(rowNumber, "Vehicle Number", vehicleNumber,
                            "Vehicle number must be 4-20 uppercase letters, digits, or hyphens"));
                    vehicleNumberError = true;
                }

                String rawCapacity = ExcelUtil.readString(row, capacityCol);
                Optional<BigDecimal> capacity = ExcelUtil.readBigDecimal(row, capacityCol);
                boolean capacityError = false;

                if (rawCapacity.isBlank()) {
                    errors.add(error(rowNumber, "Capacity", null, "Capacity is required"));
                    capacityError = true;
                } else if (capacity.isEmpty()) {
                    errors.add(error(rowNumber, "Capacity", rawCapacity, "Capacity is required and must be a valid number"));
                    capacityError = true;
                } else if (capacity.get().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(error(rowNumber, "Capacity", capacity.get().toString(), "Capacity must be greater than zero"));
                    capacityError = true;
                }

                rawRows.add(new RawVehicleRow(rowNumber, vehicleNumber, capacity.orElse(null),
                        vehicleNumberError || capacityError));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            // Duplicate detection within the uploaded file
            Set<String> seenInFile = new HashSet<>();
            for (RawVehicleRow raw : rawRows) {
                if (!raw.vehicleNumber().isBlank() && !raw.hasFormatError()) {
                    if (!seenInFile.add(raw.vehicleNumber())) {
                        errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                                "Duplicate vehicle number '" + raw.vehicleNumber() + "' in uploaded file"));
                    }
                }
            }

            // Duplicate detection against existing records in the database
            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawVehicleRow::vehicleNumber)
                    .filter(v -> !v.isBlank())
                    .collect(Collectors.toSet());

            if (!distinctVehicleNumbers.isEmpty()) {
                List<Vehicle> existingVehicles = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers);
                Set<String> existingVehicleNumbers = existingVehicles.stream()
                        .map(Vehicle::getVehicleNumber)
                        .collect(Collectors.toSet());

                for (RawVehicleRow raw : rawRows) {
                    if (existingVehicleNumbers.contains(raw.vehicleNumber())) {
                        errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                                "Vehicle number '" + raw.vehicleNumber() + "' already exists"));
                    }
                }
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Vehicle upload validation failed", errors, rawRows.size());
            }

            String actor = SecurityUtil.getCurrentUsername();
            List<Vehicle> entities = new ArrayList<>();
            for (RawVehicleRow r : rawRows) {
                entities.add(Vehicle.builder()
                        .vehicleNumber(r.vehicleNumber())
                        .capacity(r.capacity())
                        .createdBy(actor)
                        .modifiedBy(actor)
                        .build());
            }

            vehicleRepository.saveAll(entities);
            log.info("Bulk vehicle upload: {} rows inserted, by={}", entities.size(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Vehicles uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .build();
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private Integer findHeader(Map<String, Integer> headerIndex, String... possibleNames) {
        for (String name : possibleNames) {
            Integer col = headerIndex.get(name.trim().toLowerCase());
            if (col != null) {
                return col;
            }
        }
        return null;
    }

    private ExcelValidationError error(int rowNumber, String field, String value, String message) {
        return ExcelValidationError.builder()
                .rowNumber(rowNumber)
                .field(field)
                .value(value)
                .message(message)
                .build();
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + id, ErrorCodeConstants.VEHICLE_NOT_FOUND));
    }

    private BigDecimal computeAmount(Vehicle vehicle, Route route) {
        return vehicle.getCapacity()
                .multiply(route.getPrice())
                .multiply(route.getKm())
                .setScale(2, RoundingMode.HALF_UP);
    }
}

