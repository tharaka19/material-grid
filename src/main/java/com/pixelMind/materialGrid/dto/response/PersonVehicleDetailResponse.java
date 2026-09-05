package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PersonVehicleDetailResponse {
    private Long id;
    private LocalDate date;
    private PersonSummaryResponse person;
    private VehicleSummaryResponse vehicle;
    private Long fileHistoryId;
    private FileHistoryResponse fileHistory;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
