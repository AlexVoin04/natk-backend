package com.natk.natk_api.department.model;

import com.natk.natk_api.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "department")
@Getter
@Setter
@Builder
public class DepartmentEntity {
    @Id
    private UUID id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chief_id", referencedColumnName = "id")
    private UserEntity chief;
}
