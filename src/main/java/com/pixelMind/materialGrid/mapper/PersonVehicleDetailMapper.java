package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.PersonSummaryResponse;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailResponse;
import com.pixelMind.materialGrid.dto.response.VehicleSummaryResponse;
import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonVehicleDetailMapper {

    private final FileHistoryMapper fileHistoryMapper;

    public PersonVehicleDetailResponse toResponse(PersonVehicleDetail detail) {
        if (detail == null) {
            return null;
        }
        return PersonVehicleDetailResponse.builder()
                .id(detail.getId())
                .date(detail.getDate())
                .person(detail.getPerson() != null ? PersonSummaryResponse.builder()
                        .id(detail.getPerson().getId())
                        .personCode(detail.getPerson().getPersonCode())
                        .name(detail.getPerson().getName())
                        .build() : null)
                .vehicle(detail.getVehicle() != null ? VehicleSummaryResponse.builder()
                        .id(detail.getVehicle().getId())
                        .vehicleNumber(detail.getVehicle().getVehicleNumber())
                        .build() : null)
                .fileHistoryId(detail.getFileHistory() != null ? detail.getFileHistory().getId() : null)
                .fileHistory(detail.getFileHistory() != null ? fileHistoryMapper.toResponse(detail.getFileHistory()) : null)
                .createdBy(detail.getCreatedBy())
                .createdDate(detail.getCreatedDate())
                .modifiedBy(detail.getModifiedBy())
                .modifiedDate(detail.getModifiedDate())
                .build();
    }
}
