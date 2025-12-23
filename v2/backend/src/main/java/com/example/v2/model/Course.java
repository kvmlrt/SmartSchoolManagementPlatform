package com.example.v2.model;

import java.time.LocalDateTime;

public record Course(
        Integer id,
        String title,
        String description,
        Integer teacherId,
        Boolean approved,
        LocalDateTime createdAt
) {}
