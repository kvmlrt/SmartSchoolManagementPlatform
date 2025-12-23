package com.example.v2.model;

import java.time.LocalDateTime;

public record Assignment(
        Long id,
        String title,
        String description,
        Long courseId,
        LocalDateTime dueDate,
        Long createdBy,
        LocalDateTime createdAt
) {}
