package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class PersonVehicleDetailReceiptRow {
    private LocalDate date;
    private String vehicleNumber;
    private BigDecimal vehicleCapacity;
    private Integer loadCount;
    private BigDecimal totalVehicleCapacity;
}