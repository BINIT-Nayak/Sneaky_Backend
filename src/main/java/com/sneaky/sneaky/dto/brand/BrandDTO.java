package com.sneaky.sneaky.dto.brand;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BrandDTO {

    private UUID id;
    private String name;
}
