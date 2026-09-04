package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.PersonResponse;
import com.pixelMind.materialGrid.entity.Person;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

    public PersonResponse toResponse(Person person) {
        if (person == null) {
            return null;
        }
        return PersonResponse.builder()
                .id(person.getId())
                .personCode(person.getPersonCode())
                .name(person.getName())
                .personType(person.getPersonType())
                .createdBy(person.getCreatedBy())
                .createdDate(person.getCreatedDate())
                .modifiedBy(person.getModifiedBy())
                .modifiedDate(person.getModifiedDate())
                .build();
    }
}