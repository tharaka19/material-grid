package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PersonVehicleDetailService {

    PersonVehicleDetailResponse createPersonVehicleDetail(PersonVehicleDetailCreateRequest request);

    PersonVehicleDetailResponse getPersonVehicleDetail(Long id);

    Page<PersonVehicleDetailResponse> search(
            Long personId, Long vehicleId, LocalDate date, LocalDate startDate, LocalDate endDate,
            LocalDate createdDate, Long fileHistoryId, Pageable pageable);

    PersonVehicleDetailResponse updatePersonVehicleDetail(Long id, PersonVehicleDetailUpdateRequest request);

    void deletePersonVehicleDetail(Long id);
}
