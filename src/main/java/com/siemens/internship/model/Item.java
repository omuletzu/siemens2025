package com.siemens.internship.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;

    // aceasta ar fi valoarea default inainte ca item-ul sa fie procesat
    private String status = "NEPROCESAT";

    @Pattern(regexp = "^[a-zA-Z0-9-\\.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,4}$", message = "Wrong email format")
    private String email;
}