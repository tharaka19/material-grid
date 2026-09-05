package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PersonVehicleDetailReceipt {
    private String personCode;
    private String personName;
    private LocalDate startDate;
    private LocalDate endDate;

    private List<PersonVehicleDetailReceiptRow> rows;
    private BigDecimal grandTotalCapacity;
}