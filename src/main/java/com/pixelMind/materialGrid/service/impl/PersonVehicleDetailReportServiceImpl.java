package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceiptRow;
import com.pixelMind.materialGrid.entity.Person;
import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.repository.PersonRepository;
import com.pixelMind.materialGrid.repository.PersonVehicleDetailRepository;
import com.pixelMind.materialGrid.service.PersonVehicleDetailReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CONSOLIDATION KEY: (date, vehicle) - not vehicle alone. The feature
 * spec's own worked example shows the same vehicle appearing as separate
 * rows on different dates (not merged across the whole range), so this
 * follows that example literally rather than the more ambiguous section
 * heading ("consolidated by Vehicle Number") - see this feature's
 * architectural notes for the full explanation.
 *
 * Every method here is read-only: generating a receipt must never create,
 * update, or delete any row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonVehicleDetailReportServiceImpl implements PersonVehicleDetailReportService {

    private final PersonRepository personRepository;
    private final PersonVehicleDetailRepository personVehicleDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public PersonVehicleDetailReceipt generateReceipt(Long personId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    "Start date cannot be greater than end date.", ErrorCodeConstants.VALIDATION_FAILED);
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found.", ErrorCodeConstants.PERSON_NOT_FOUND));

        List<PersonVehicleDetail> details =
                personVehicleDetailRepository.findByPersonIdAndDateBetweenForReport(personId, startDate, endDate);
        if (details.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No person vehicle details found for the selected person and date range.",
                    ErrorCodeConstants.PERSON_VEHICLE_DETAIL_NOT_FOUND);
        }

        List<PersonVehicleDetailReceiptRow> rows = buildConsolidatedRows(details);

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (PersonVehicleDetailReceiptRow row : rows) {
            grandTotal = grandTotal.add(row.getTotalVehicleCapacity());
        }

        log.info("Person vehicle detail receipt generated: personId={}, startDate={}, endDate={}, rows={}, grandTotal={}",
                personId, startDate, endDate, rows.size(), grandTotal);

        return PersonVehicleDetailReceipt.builder()
                .personCode(person.getPersonCode())
                .personName(person.getName())
                .startDate(startDate)
                .endDate(endDate)
                .rows(rows)
                .grandTotalCapacity(grandTotal)
                .build();
    }

    /**
     * Groups by (date, vehicleId) - `details` is already ordered by date
     * then vehicle number ascending (see the repository query), so
     * first-insertion order into the LinkedHashMap matches that same order,
     * no separate sort needed.
     */
    private List<PersonVehicleDetailReceiptRow> buildConsolidatedRows(List<PersonVehicleDetail> details) {
        record GroupKey(LocalDate date, Long vehicleId) {
        }

        Map<GroupKey, List<PersonVehicleDetail>> grouped = new LinkedHashMap<>();
        for (PersonVehicleDetail detail : details) {
            GroupKey key = new GroupKey(detail.getDate(), detail.getVehicle().getId());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(detail);
        }

        List<PersonVehicleDetailReceiptRow> rows = new ArrayList<>(grouped.size());
        for (Map.Entry<GroupKey, List<PersonVehicleDetail>> entry : grouped.entrySet()) {
            List<PersonVehicleDetail> group = entry.getValue();
            PersonVehicleDetail representative = group.getFirst();
            int loadCount = group.size();
            BigDecimal capacity = representative.getVehicle().getCapacity();
            BigDecimal totalCapacity = capacity.multiply(BigDecimal.valueOf(loadCount));

            rows.add(PersonVehicleDetailReceiptRow.builder()
                    .date(entry.getKey().date())
                    .vehicleNumber(representative.getVehicle().getVehicleNumber())
                    .vehicleCapacity(capacity)
                    .loadCount(loadCount)
                    .totalVehicleCapacity(totalCapacity)
                    .build());
        }
        return rows;
    }
}