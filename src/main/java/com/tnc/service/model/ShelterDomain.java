package com.tnc.service.model;

import com.tnc.repository.animal.Animal;
import com.tnc.service.validation.OnCreate;

import javax.validation.constraints.*;
import java.util.List;

public record ShelterDomain(
        @NotNull(message = "Id must not be null", groups = OnCreate.class)
        @Null(message = "Id must be null", groups = OnCreate.class)
        @Positive
        Long id,
        @NotNull
        @NotBlank
        @Size(min = 3, max = 50, message = "The name must be between 3 and 50 chars")
        String name,
        @NotNull
        @NotBlank
        String address,
        @NotNull
        @NotBlank
        List<Animal> animalList
) {
}
