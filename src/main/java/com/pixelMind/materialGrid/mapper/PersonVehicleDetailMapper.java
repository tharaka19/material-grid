package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.PersonSummaryResponse;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailResponse;
import com.pixelMind.materialGrid.dto.response.VehicleSummaryResponse;
import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import org.springframework.stereotype.Component;

@Component
public class PersonVehicleDetailMapper {

    public PersonVehicleDetailResponse toResponse(PersonVehicleDetail detail) {
        if (detail == null) {
            return null;
        }
        return PersonVehicleDetailResponse.builder()
                .id(detail.getId())
                .date(detail.getDate())
                .person(PersonSummaryResponse.builder()
                        .id(detail.getPerson().getId())
                        .personCode(detail.getPerson().getPersonCode())
                        .name(detail.getPerson().getName())
                        .build())
                .vehicle(VehicleSummaryResponse.builder()
                        .id(detail.getVehicle().getId())
                        .vehicleNumber(detail.getVehicle().getVehicleNumber())
                        .build())
                .createdBy(detail.getCreatedBy())
                .createdDate(detail.getCreatedDate())
                .modifiedBy(detail.getModifiedBy())
                .modifiedDate(detail.getModifiedDate())
                .build();
    }
}