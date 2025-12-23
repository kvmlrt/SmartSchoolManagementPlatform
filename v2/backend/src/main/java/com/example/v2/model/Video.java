package com.example.v2.model;

import java.time.LocalDateTime;

public record Video(
        Long id,
        Integer courseId,
        Integer teacherId,
        String filename,
        String path,
        String thumbnailPath,
        String title,
        String contentType,
        Long size,
        LocalDateTime uploadedAt,
        Integer approved,
        String reviewRemark,
        LocalDateTime reviewedAt
) {}
