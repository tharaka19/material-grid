package com.pixelMind.materialGrid.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PersonVehicleDetailUpdateRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Person id is required")
    private Long personId;

    @NotNull(message = "Vehicle id is required")
    private Long vehicleId;
}