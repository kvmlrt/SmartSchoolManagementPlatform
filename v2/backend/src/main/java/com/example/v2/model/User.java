package com.example.v2.model;

public record User(
        Integer id,
        String username,
        String role,
        Integer classId,
        String studentNumber
) {}
