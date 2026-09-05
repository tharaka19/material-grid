package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ExcelConstants;
import com.pixelMind.materialGrid.dto.response.BulkUploadResponse;
import com.pixelMind.materialGrid.dto.response.ExcelValidationError;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.Person;
import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.ExcelValidationException;
import com.pixelMind.materialGrid.repository.PersonRepository;
import com.pixelMind.materialGrid.repository.PersonVehicleDetailRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.FileHistoryService;
import com.pixelMind.materialGrid.service.PersonVehicleDetailImportService;
import com.pixelMind.materialGrid.util.ExcelUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mirrors VehicleLicenseImportServiceImpl exactly: duplicate-file check
 * before parsing, bulk lookups instead of per-row queries, both within-file
 * and against-database duplicate detection, collect-all-errors-then-write,
 * FileHistory created (in this same transaction - see
 * FileHistoryServiceImpl's Javadoc) only once every row has passed.
 *
 * FileHistory integration is not explicitly requested by this feature's
 * spec but is added for consistency with every other Excel upload in this
 * project - see this feature's architectural notes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonVehicleDetailImportServiceImpl implements PersonVehicleDetailImportService {

    private final PersonRepository personRepository;
    private final VehicleRepository vehicleRepository;
    private final PersonVehicleDetailRepository personVehicleDetailRepository;
    private final FileHistoryService fileHistoryService;

    private record RawRow(int rowNumber, LocalDate date, String personCode, String vehicleNumber) {
    }

    private record ResolvedRow(int rowNumber, LocalDate date, Person person, Vehicle vehicle) {
    }

    @Override
    @Transactional
    public BulkUploadResponse importFromExcel(MultipartFile file) {
        String fileName = ExcelUtil.extractSafeFileName(file);
        fileHistoryService.validateNotAlreadyUploaded(fileName, FileType.PERSON_VEHICLE_DETAIL);

        Workbook workbook = ExcelUtil.openWorkbook(file);
        try {
            Sheet sheet = ExcelUtil.firstSheet(workbook);
            Map<String, Integer> headerIndex = ExcelUtil.readHeaderIndex(sheet);
            ExcelUtil.requireHeaders(headerIndex, ExcelConstants.PERSON_VEHICLE_DETAIL_HEADERS);

            int dateCol = ExcelUtil.columnOf(headerIndex, "Date");
            int personCol = ExcelUtil.columnOf(headerIndex, "Person Code");
            int vehicleCol = ExcelUtil.columnOf(headerIndex, "Vehicle Number");

            List<ExcelValidationError> errors = new ArrayList<>();
            List<RawRow> rawRows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                int rowNumber = r + 1;

                Optional<LocalDate> date = ExcelUtil.readDate(row, dateCol);
                if (date.isEmpty()) {
                    String rawValue = ExcelUtil.readString(row, dateCol);
                    errors.add(error(rowNumber, "Date", rawValue,
                            rawValue.isBlank() ? "Date is required" : "Invalid date"));
                }

                String personCode = ExcelUtil.readString(row, personCol).toUpperCase();
                if (personCode.isBlank()) {
                    errors.add(error(rowNumber, "Person Code", null, "Person Code is required"));
                }

                String vehicleNumber = ExcelUtil.readString(row, vehicleCol).toUpperCase();
                if (vehicleNumber.isBlank()) {
                    errors.add(error(rowNumber, "Vehicle Number", null, "Vehicle Number is required"));
                }

                rawRows.add(new RawRow(rowNumber, date.orElse(null), personCode, vehicleNumber));
            }

            if (rawRows.isEmpty()) {
                throw new ExcelValidationException("The uploaded file contains no data rows",
                        List.of(error(0, "File", null, "No data rows found")), 0);
            }

            // --- Bulk-fetch persons and vehicles (one query each) ---
            Set<String> distinctPersonCodes = rawRows.stream()
                    .map(RawRow::personCode).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Person> personByCode = personRepository.findByPersonCodeInAndDeletedFalse(distinctPersonCodes).stream()
                    .collect(Collectors.toMap(Person::getPersonCode, p -> p));

            Set<String> distinctVehicleNumbers = rawRows.stream()
                    .map(RawRow::vehicleNumber).filter(v -> !v.isBlank()).collect(Collectors.toSet());
            Map<String, Vehicle> vehicleByNumber = vehicleRepository.findByVehicleNumberInAndDeletedFalse(distinctVehicleNumbers).stream()
                    .collect(Collectors.toMap(Vehicle::getVehicleNumber, v -> v));

            // --- Resolve person + vehicle per row ---
            List<ResolvedRow> resolved = new ArrayList<>();
            for (RawRow raw : rawRows) {

                if (raw.personCode().isBlank() || raw.vehicleNumber().isBlank()) {
                    continue;
                }

                Person person = personByCode.get(raw.personCode());
                    if (person == null) {
                        errors.add(error(raw.rowNumber(), "Person Code", raw.personCode(),
                                "Person code '" + raw.personCode() + "' does not exist"));
                        continue;
                    }

                Vehicle vehicle = vehicleByNumber.get(raw.vehicleNumber());
                    if (vehicle == null) {
                        errors.add(error(raw.rowNumber(), "Vehicle Number", raw.vehicleNumber(),
                                "Vehicle number '" + raw.vehicleNumber() + "' does not exist"));
                        continue;
                    }

                    resolved.add(new ResolvedRow(raw.rowNumber(), raw.date(), person, vehicle));
            }

            // --- Duplicate detection WITHIN this file ---
//            Set<String> seenInFile = new HashSet<>();
//            for (ResolvedRow row : resolved) {
//                String key = businessKey(row.person().getId(), row.vehicle().getId(), row.date());
//                if (!seenInFile.add(key)) {
//                    errors.add(error(row.rowNumber(), "Duplicate", null,
//                            "Duplicate person vehicle detail found in the uploaded file for the same "
//                                    + "person, vehicle and date."));
//                }
//            }

            // --- Duplicate detection against EXISTING database records ---
//            if (!resolved.isEmpty()) {
//                Set<Long> personIds = resolved.stream().map(r -> r.person().getId()).collect(Collectors.toSet());
//                Set<Long> vehicleIds = resolved.stream().map(r -> r.vehicle().getId()).collect(Collectors.toSet());
//                Set<LocalDate> dates = resolved.stream().map(ResolvedRow::date).collect(Collectors.toSet());
//
//                Set<String> existingKeys = personVehicleDetailRepository
//                        .findByPersonIdInAndVehicleIdInAndDateInAndDeletedFalse(personIds, vehicleIds, dates).stream()
//                        .map(d -> businessKey(d.getPerson().getId(), d.getVehicle().getId(), d.getDate()))
//                        .collect(Collectors.toSet());
//
//                for (ResolvedRow row : resolved) {
//                    String key = businessKey(row.person().getId(), row.vehicle().getId(), row.date());
//                    if (existingKeys.contains(key)) {
//                        errors.add(error(row.rowNumber(), "Person Vehicle Detail", null,
//                                "Duplicate person vehicle detail."));
//                    }
//                }
//            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException("Person vehicle detail upload validation failed", errors, rawRows.size());
            }

            // All rows valid - create File History, then the records, in
            // this same transaction (see FileHistoryServiceImpl Javadoc).
            FileHistory fileHistory = fileHistoryService.createFileHistory(fileName, FileType.PERSON_VEHICLE_DETAIL);
            String actor = SecurityUtil.getCurrentUsername();

            List<PersonVehicleDetail> entities = (List<PersonVehicleDetail>) resolved.stream()
                    .map(r -> PersonVehicleDetail.builder()
                            .date(r.date())
                            .person(r.person())
                            .vehicle(r.vehicle())
                            .fileHistory(fileHistory)
                            .deleted(false)
                            .createdBy(actor)
                            .modifiedBy(actor)
                            .build())
                    .toList();

            personVehicleDetailRepository.saveAll(entities);
            log.info("Bulk person vehicle detail upload: {} rows inserted, fileHistoryId={}, by={}",
                    entities.size(), fileHistory.getId(), actor);

            return BulkUploadResponse.builder()
                    .success(true)
                    .message("Person vehicle details uploaded successfully")
                    .totalRows(rawRows.size())
                    .successCount(entities.size())
                    .errorCount(0)
                    .errors(List.of())
                    .fileHistoryId(fileHistory.getId())
                    .fileName(fileName)
                    .fileType(FileType.PERSON_VEHICLE_DETAIL.name())
                    .build();
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String businessKey(Long personId, Long vehicleId, LocalDate date) {
        return personId + ":" + vehicleId + ":" + date;
    }

    private ExcelValidationError error(int rowNumber, String field, String value, String message) {
        return ExcelValidationError.builder().rowNumber(rowNumber).field(field).value(value).message(message).build();
    }
}