package com.example.v2.controller;

import com.example.v2.dto.StudentGradesDto;
import com.example.v2.model.ClassInfo;
import com.example.v2.model.GradeLevel;
import com.example.v2.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/grade-levels")
    public List<GradeLevel> gradeLevels() {
        return reports.getAllGradeLevels();
    }

    @GetMapping("/grade-levels/{id}/classes")
    public ResponseEntity<List<ClassInfo>> classesByGrade(@PathVariable("id") Long id) {
        return ResponseEntity.ok(reports.getClassesByGradeLevel(id));
    }

    @GetMapping("/classes/{classId}/student-grades")
    public ResponseEntity<List<StudentGradesDto>> studentGrades(@PathVariable("classId") Long classId) {
        return ResponseEntity.ok(reports.getStudentGradesByClass(classId));
    }

    @GetMapping("/student-grades")
    public ResponseEntity<List<StudentGradesDto>> allStudentGrades() {
        return ResponseEntity.ok(reports.getAllStudentGrades());
    }
}
