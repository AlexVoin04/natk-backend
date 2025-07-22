package com.example.natk_auth.dto;

import java.util.List;

public record UserCredentialsDto(String login, String password, String name, String surname, String patronymic, List<String> roles) {}