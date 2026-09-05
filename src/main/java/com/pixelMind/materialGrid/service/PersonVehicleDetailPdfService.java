package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;

public interface PersonVehicleDetailPdfService {

    /** Pure rendering - accepts an already-built receipt DTO and returns
     * PDF bytes. Performs no database access. */
    byte[] generatePdf(PersonVehicleDetailReceipt receipt);
}