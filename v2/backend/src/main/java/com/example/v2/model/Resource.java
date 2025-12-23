package com.example.v2.model;

import java.time.LocalDateTime;

public record Resource(
        Long id,
        String name,
        String url,
        byte[] data,
        String contentType,
        LocalDateTime uploadedAt,
        String filePath
) {}
