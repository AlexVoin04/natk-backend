package com.example.natk_auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

}
