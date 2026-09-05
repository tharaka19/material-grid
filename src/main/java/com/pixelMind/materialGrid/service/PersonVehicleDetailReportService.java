package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;

import java.time.LocalDate;

public interface PersonVehicleDetailReportService {

    /**
     * Read-only. Validates the person exists and startDate <= endDate,
     * resolves every PersonVehicleDetail for (personId, startDate..endDate)
     * inclusive, CONSOLIDATES them by (date, vehicle) - see impl for the
     * grouping rule - and computes the grand total.
     */
    PersonVehicleDetailReceipt generateReceipt(Long personId, LocalDate startDate, LocalDate endDate);
}