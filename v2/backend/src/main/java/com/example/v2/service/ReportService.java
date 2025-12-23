package com.example.v2.service;

import com.example.v2.dto.StudentGradesDto;
import com.example.v2.model.ClassInfo;
import com.example.v2.model.Grade;
import com.example.v2.model.GradeLevel;
import com.example.v2.repository.GradeRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private final NamedParameterJdbcTemplate jdbc;
    private final GradeRepository grades;

    public ReportService(NamedParameterJdbcTemplate jdbc, GradeRepository grades) {
        this.jdbc = jdbc;
        this.grades = grades;
    }

    public List<StudentGradesDto> getStudentGradesByClass(Long classId) {
        if (classId == null) return List.of();
        String sql = """
                SELECT u.id AS id, u.username AS username, u.student_number AS student_number,
                       c.name AS class_name, gl.name AS grade_level
                FROM users u
                LEFT JOIN classes c ON u.class_id = c.id
                LEFT JOIN grade_levels gl ON c.grade_level_id = gl.id
                WHERE u.class_id = :classId
                """;
        var params = new MapSqlParameterSource("classId", classId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return mapStudents(rows);
    }

    public List<StudentGradesDto> getAllStudentGrades() {
        String sql = """
                SELECT u.id AS id, u.username AS username, u.student_number AS student_number,
                       c.name AS class_name, gl.name AS grade_level
                FROM users u
                LEFT JOIN classes c ON u.class_id = c.id
                LEFT JOIN grade_levels gl ON c.grade_level_id = gl.id
                WHERE u.role = 'STUDENT' OR u.student_number IS NOT NULL
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource());
        return mapStudents(rows);
    }

    public List<GradeLevel> getAllGradeLevels() {
        String sql = "SELECT id, name, parent_id FROM grade_levels ORDER BY id";
        return jdbc.query(sql, (rs, i) -> new GradeLevel(
                rs.getObject("id", Long.class),
                rs.getString("name"),
                rs.getObject("parent_id", Long.class)
        ));
    }

    public List<ClassInfo> getClassesByGradeLevel(Long gradeLevelId) {
        if (gradeLevelId == null) return List.of();
        String sql = "SELECT id, name FROM classes WHERE grade_level_id = :gradeLevelId ORDER BY id";
        var params = new MapSqlParameterSource("gradeLevelId", gradeLevelId);
        return jdbc.query(sql, params, (rs, i) -> new ClassInfo(
                rs.getObject("id", Long.class),
                rs.getString("name"),
                gradeLevelId
        ));
    }

    private List<StudentGradesDto> mapStudents(List<Map<String, Object>> rows) {
        List<StudentGradesDto> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Integer studentId = r.get("id") == null ? null : ((Number) r.get("id")).intValue();
            String username = (String) r.get("username");
            String studentNumber = r.get("student_number") == null ? null : r.get("student_number").toString();
            String className = (String) r.get("class_name");
            String gradeLevel = (String) r.get("grade_level");
            List<Grade> gradeRows = loadGradesForStudent(studentId);
            result.add(new StudentGradesDto(studentId, studentNumber, username, gradeLevel, className, gradeRows));
        }
        return result;
    }

    private List<Grade> loadGradesForStudent(Integer studentId) {
        if (studentId == null) return List.of();
        return grades.findByStudent(studentId);
    }
}
