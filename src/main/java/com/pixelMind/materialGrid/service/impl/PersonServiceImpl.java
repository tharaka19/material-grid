package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.CodeSequenceConstants;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.PersonCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PersonResponse;
import com.pixelMind.materialGrid.entity.Person;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.PersonMapper;
import com.pixelMind.materialGrid.repository.PersonRepository;
import com.pixelMind.materialGrid.repository.PersonVehicleDetailRepository;
import com.pixelMind.materialGrid.service.PersonService;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonVehicleDetailRepository personVehicleDetailRepository;
    private final PersonMapper personMapper;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional
    public PersonResponse createPerson(PersonCreateRequest request) {

        String name = request.getName().trim();

        if (personRepository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new DuplicateResourceException(
                    "Person already exists with name: " + name,
                    ErrorCodeConstants.DUPLICATE_PERSON_NAME);
        }

        String actor = SecurityUtil.getCurrentUsername();

        String personCode = codeGeneratorService.nextCode(
                CodeSequenceConstants.PERSON_CODE_SEQUENCE,
                CodeSequenceConstants.PERSON_CODE_PREFIX,
                CodeSequenceConstants.PERSON_CODE_PAD_LENGTH);

        Person person = Person.builder()
                .personCode(personCode)
                .name(name)
                .personType(request.getPersonType())
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        Person saved = personRepository.save(person);
        log.info("Person created: id={}, personCode={}, by={}", saved.getId(), saved.getPersonCode(), actor);
        return personMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse getPerson(Long id) {
        return personMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonResponse> getPersons(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return personRepository.searchByNameOrPersonCode(search.trim(), pageable).map(personMapper::toResponse);
        }
        return personRepository.findAllByDeletedFalse(pageable).map(personMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonResponse updatePerson(Long id, PersonUpdateRequest request) {

        Person person = findOrThrow(id);

        if (personVehicleDetailRepository.existsByPersonIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Cannot update person with existing person vehicle detail records. "
                            + "This person has historical transaction records and cannot be modified.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }

        String name = request.getName().trim();

        if (personRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(name, id)) {
            throw new DuplicateResourceException(
                    "Person already exists with name: " + name,
                    ErrorCodeConstants.DUPLICATE_PERSON_NAME);
        }

        person.setName(name);
        person.setPersonType(request.getPersonType());
        person.setModifiedBy(SecurityUtil.getCurrentUsername());
        // personCode is never touched here - immutable by design.

        Person saved = personRepository.save(person);
        log.info("Person updated: id={}, by={}", saved.getId(), person.getModifiedBy());
        return personMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePerson(Long id) {

        Person person = findOrThrow(id);

        if (personVehicleDetailRepository.existsByPersonIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Cannot delete person with existing person vehicle detail records. "
                            + "These are historical records and this person must be preserved for referential integrity.",
                    ErrorCodeConstants.BUSINESS_RULE_VIOLATION);
        }

        person.setDeleted(true);
        person.setModifiedBy(SecurityUtil.getCurrentUsername());
        personRepository.save(person);
        log.info("Person deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private Person findOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Person not found.", ErrorCodeConstants.PERSON_NOT_FOUND));
    }
}