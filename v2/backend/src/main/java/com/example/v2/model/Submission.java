package com.example.v2.model;

import java.time.LocalDateTime;

public record Submission(
        Long id,
        Long assignmentId,
        Long studentId,
        String content,
        String filePath,
        String fileType,
        Double grade,
        Long gradedBy,
        LocalDateTime gradedAt,
        String status,
        String remarks,
        LocalDateTime submittedAt
) {}
