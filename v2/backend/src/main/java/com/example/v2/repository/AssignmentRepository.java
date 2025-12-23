package com.example.v2.repository;

import com.example.v2.model.Assignment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Assignment> mapper = new RowMapper<Assignment>() {
        @Override
        public Assignment mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Assignment(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getObject("course_id", Long.class),
                    rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toLocalDateTime() : null,
                    rs.getObject("created_by", Long.class),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
            );
        }
    };

    public AssignmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private void ensureTable() {
        jdbc.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS assignments (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(255)," +
                "description TEXT," +
                "course_id BIGINT," +
                "due_date DATETIME," +
                "created_by BIGINT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public List<Assignment> findAll() {
        ensureTable();
        return jdbc.query("select * from assignments order by created_at desc", mapper);
    }

    public List<Assignment> findByTeacherId(Long teacherId) {
        ensureTable();
        var params = new MapSqlParameterSource("teacherId", teacherId);
        return jdbc.query("select * from assignments where created_by=:teacherId order by created_at desc", params, mapper);
    }

    public Optional<Assignment> findById(Long id) {
        ensureTable();
        var params = new MapSqlParameterSource("id", id);
        List<Assignment> list = jdbc.query("select * from assignments where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public Assignment save(Assignment a) {
        ensureTable();
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("title", a.title())
                .addValue("description", a.description())
                .addValue("courseId", a.courseId())
                .addValue("dueDate", a.dueDate())
                .addValue("createdBy", a.createdBy());
        jdbc.update("INSERT INTO assignments (title, description, course_id, due_date, created_by, created_at) " +
                "VALUES (:title, :description, :courseId, :dueDate, :createdBy, NOW())", params, kh);
        Long id = kh.getKey() != null ? kh.getKey().longValue() : null;
        return new Assignment(id, a.title(), a.description(), a.courseId(), a.dueDate(), a.createdBy(), java.time.LocalDateTime.now());
    }

    public List<Assignment> findByClassId(Long classId) {
        // Select assignments that have submissions from students in the class
        var params = new MapSqlParameterSource("classId", classId);
        String sql = "SELECT DISTINCT a.* FROM assignments a " +
                "JOIN submissions s ON s.assignment_id = a.id " +
                "JOIN users u ON u.id = s.student_id " +
                "WHERE u.class_id = :classId " +
                "ORDER BY a.created_at DESC";
        ensureTable();
        return jdbc.query(sql, params, mapper);
    }
}
