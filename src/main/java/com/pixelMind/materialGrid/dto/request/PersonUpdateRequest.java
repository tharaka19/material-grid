package com.pixelMind.materialGrid.dto.request;

import com.pixelMind.materialGrid.entity.enums.PersonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PersonUpdateRequest {

    @NotBlank(message = "Person name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Person type is required")
    private PersonType personType;

}