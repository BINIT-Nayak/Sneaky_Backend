package com.sneaky.sneaky.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductColor {

    @Column(name = "color_name", nullable = false)
    private String name;

    @Column(name = "color_value", nullable = false)
    private String value;
}
