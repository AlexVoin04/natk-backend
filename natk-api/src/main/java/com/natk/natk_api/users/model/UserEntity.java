package com.natk.natk_api.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column
    private UUID id;
    @Column
    private String login;
    @Column
    private String password;
    @Column
    private String name;
    @Column
    private String surname;
    @Column
    private String patronymic;
    @Column
    private String phoneNumber;

    @ManyToMany(fetch = FetchType.EAGER) // EAGER — чтобы сразу загружались роли
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<RoleEntity> roles = new ArrayList<>();

    public String getShortFio() {
        StringBuilder sb = new StringBuilder();

        if (surname != null && !surname.isBlank()) {
            sb.append(surname);
        }

        if (name != null && !name.isBlank()) {
            sb.append(" ").append(name.charAt(0)).append(".");
        }

        if (patronymic != null && !patronymic.isBlank()) {
            sb.append(patronymic.charAt(0)).append(".");
        }

        return sb.toString();
    }
}
