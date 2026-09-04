package com.pixelMind.materialGrid.dto.response;

import com.pixelMind.materialGrid.entity.enums.PersonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PersonResponse {
    private Long id;
    private String personCode;
    private String name;
    private PersonType personType;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}