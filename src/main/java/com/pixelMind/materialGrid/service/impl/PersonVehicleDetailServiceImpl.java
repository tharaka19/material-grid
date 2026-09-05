package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonVehicleDetailUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailResponse;
import com.pixelMind.materialGrid.entity.Person;
import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import com.pixelMind.materialGrid.entity.Vehicle;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.PersonVehicleDetailMapper;
import com.pixelMind.materialGrid.repository.PersonRepository;
import com.pixelMind.materialGrid.repository.PersonVehicleDetailRepository;
import com.pixelMind.materialGrid.repository.VehicleRepository;
import com.pixelMind.materialGrid.service.PersonVehicleDetailService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonVehicleDetailServiceImpl implements PersonVehicleDetailService {

    private final PersonVehicleDetailRepository personVehicleDetailRepository;
    private final PersonRepository personRepository;
    private final VehicleRepository vehicleRepository;
    private final PersonVehicleDetailMapper personVehicleDetailMapper;

    @Override
    @Transactional
    public PersonVehicleDetailResponse createPersonVehicleDetail(PersonVehicleDetailCreateRequest request) {
        Person person = findPersonOrThrow(request.getPersonId());
        Vehicle vehicle = findVehicleOrThrow(request.getVehicleId());

        if (personVehicleDetailRepository.existsByPersonIdAndVehicleIdAndDateAndDeletedFalse(
                person.getId(), vehicle.getId(), request.getDate())) {
            throw new DuplicateResourceException(
                    "A person vehicle detail already exists for this person, vehicle and date.",
                    ErrorCodeConstants.DUPLICATE_PERSON_VEHICLE_DETAIL);
        }

        String actor = SecurityUtil.getCurrentUsername();
        PersonVehicleDetail detail = PersonVehicleDetail.builder()
                .date(request.getDate())
                .person(person)
                .vehicle(vehicle)
                .deleted(false)
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        PersonVehicleDetail saved = personVehicleDetailRepository.save(detail);
        log.info("PersonVehicleDetail created: id={}, personId={}, vehicleId={}, by={}",
                saved.getId(), person.getId(), vehicle.getId(), actor);
        return personVehicleDetailMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonVehicleDetailResponse getPersonVehicleDetail(Long id) {
        return personVehicleDetailMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonVehicleDetailResponse> search(
            Long personId, Long vehicleId, LocalDate date, LocalDate startDate, LocalDate endDate,
            LocalDate createdDate, Long fileHistoryId, Pageable pageable) {
        LocalDateTime createdDateFrom = createdDate != null ? createdDate.atStartOfDay() : null;
        LocalDateTime createdDateTo = createdDate != null ? createdDate.plusDays(1).atStartOfDay() : null;

        return personVehicleDetailRepository.search(
                personId, vehicleId, date, startDate, endDate, createdDateFrom, createdDateTo, fileHistoryId, pageable)
                .map(personVehicleDetailMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonVehicleDetailResponse updatePersonVehicleDetail(Long id, PersonVehicleDetailUpdateRequest request) {
        PersonVehicleDetail detail = findOrThrow(id);
        Person person = findPersonOrThrow(request.getPersonId());
        Vehicle vehicle = findVehicleOrThrow(request.getVehicleId());

        if (personVehicleDetailRepository.existsByPersonIdAndVehicleIdAndDateAndDeletedFalseAndIdNot(
                person.getId(), vehicle.getId(), request.getDate(), id)) {
            throw new DuplicateResourceException(
                    "A person vehicle detail already exists for this person, vehicle and date.",
                    ErrorCodeConstants.DUPLICATE_PERSON_VEHICLE_DETAIL);
        }

        detail.setDate(request.getDate());
        detail.setPerson(person);
        detail.setVehicle(vehicle);
        detail.setModifiedBy(SecurityUtil.getCurrentUsername());

        PersonVehicleDetail saved = personVehicleDetailRepository.save(detail);
        log.info("PersonVehicleDetail updated: id={}, by={}", saved.getId(), detail.getModifiedBy());
        return personVehicleDetailMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePersonVehicleDetail(Long id) {
        PersonVehicleDetail detail = findOrThrow(id);
        detail.setDeleted(true);
        detail.setModifiedBy(SecurityUtil.getCurrentUsername());
        personVehicleDetailRepository.save(detail);
        log.info("PersonVehicleDetail soft-deleted: id={}, by={}", id, detail.getModifiedBy());
    }

    private Person findPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Person not found.", ErrorCodeConstants.PERSON_NOT_FOUND));
    }

    private Vehicle findVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found.", ErrorCodeConstants.VEHICLE_NOT_FOUND));
    }

    private PersonVehicleDetail findOrThrow(Long id) {
        return personVehicleDetailRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Person vehicle detail not found.", ErrorCodeConstants.PERSON_VEHICLE_DETAIL_NOT_FOUND));
    }
}