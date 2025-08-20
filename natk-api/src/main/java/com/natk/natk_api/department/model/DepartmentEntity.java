package com.natk.natk_api.department.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "department")
@Getter
@Setter
public class DepartmentEntity {
    @Id
    private UUID id;

    private String name;

    @Column(name = "chief_id")
    private UUID chiefId;
}
