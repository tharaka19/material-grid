package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.CodeSequenceConstants;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.PersonCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PersonResponse;
import com.pixelMind.materialGrid.entity.Person;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.PersonMapper;
import com.pixelMind.materialGrid.repository.PersonRepository;
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
    private final PersonMapper personMapper;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional
    public PersonResponse createPerson(PersonCreateRequest request) {

        // TODO: Validate person to avoid duplicate person names

        String actor = SecurityUtil.getCurrentUsername();

        String personCode = codeGeneratorService.nextCode(
                CodeSequenceConstants.PERSON_CODE_SEQUENCE,
                CodeSequenceConstants.PERSON_CODE_PREFIX,
                CodeSequenceConstants.PERSON_CODE_PAD_LENGTH);

        Person person = Person.builder()
                .personCode(personCode)
                .name(request.getName().trim())
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
            return personRepository.findByNameContainingIgnoreCaseAndDeletedFalse(search, pageable).map(personMapper::toResponse);
        }
        return personRepository.findAll(pageable).map(personMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonResponse updatePerson(Long id, PersonUpdateRequest request) {

        // TODO: Is exists any as escavator or checking records, need special authority to update this

        Person person = findOrThrow(id);
        person.setName(request.getName().trim());
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

        // TODO: Is exists any as escavator or checking records, must avoid delete

        Person person = findOrThrow(id);
        personRepository.delete(person);
        log.info("Person deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private Person findOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Person not found.", ErrorCodeConstants.PERSON_NOT_FOUND));
    }
}