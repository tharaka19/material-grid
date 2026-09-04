package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.PersonCreateRequest;
import com.pixelMind.materialGrid.dto.request.PersonUpdateRequest;
import com.pixelMind.materialGrid.dto.response.PersonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonService {

    PersonResponse createPerson(PersonCreateRequest request);

    PersonResponse getPerson(Long id);

    Page<PersonResponse> getPersons(String search, Pageable pageable);

    PersonResponse updatePerson(Long id, PersonUpdateRequest request);

    void deletePerson(Long id);
}