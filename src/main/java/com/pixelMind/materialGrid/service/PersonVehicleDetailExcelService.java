package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;

public interface PersonVehicleDetailExcelService {

    /**
     * Pure rendering - accepts an already-built receipt DTO (the exact same
     * one PersonVehicleDetailPdfService consumes) and returns .xlsx bytes.
     * Performs no database access and no data retrieval/consolidation of
     * its own - see this feature's architectural notes.
     */
    byte[] generateExcel(PersonVehicleDetailReceipt receipt);
}