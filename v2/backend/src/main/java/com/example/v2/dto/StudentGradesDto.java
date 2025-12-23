package com.example.v2.dto;

import com.example.v2.model.Grade;

import java.util.List;

public record StudentGradesDto(
        Integer studentId,
        String studentNo,
        String name,
        String gradeLevel,
        String className,
        List<Grade> grades
) {}
