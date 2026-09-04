package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.LicenseResponse;
import com.pixelMind.materialGrid.entity.License;
import org.springframework.stereotype.Component;

@Component
public class LicenseMapper {

    public LicenseResponse toResponse(License license) {
        if (license == null) {
            return null;
        }
        return LicenseResponse.builder()
                .id(license.getId())
                .licenseCode(license.getLicenseCode())
                .startDate(license.getStartDate())
                .endDate(license.getEndDate())
                .price(license.getPrice())
                .createdBy(license.getCreatedBy())
                .createdDate(license.getCreatedDate())
                .modifiedBy(license.getModifiedBy())
                .modifiedDate(license.getModifiedDate())
                .build();
    }
}
