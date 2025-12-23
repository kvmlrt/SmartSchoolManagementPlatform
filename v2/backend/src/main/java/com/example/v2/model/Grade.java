package com.example.v2.model;

import java.time.LocalDateTime;

public record Grade(
        Integer id,
        Integer studentId,
        Integer courseId,
        Double score,
        Integer modifiedBy,
        LocalDateTime modifiedAt,
        Long classId,
        String type,
        String remarks
) {}
