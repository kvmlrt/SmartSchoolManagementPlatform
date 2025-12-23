package com.example.v2.repository;

import com.example.v2.model.Submission;
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
public class SubmissionRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Submission> mapper = new RowMapper<Submission>() {
        @Override
        public Submission mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Submission(
                    rs.getLong("id"),
                    rs.getObject("assignment_id", Long.class),
                    rs.getObject("student_id", Long.class),
                    rs.getString("content"),
                    rs.getString("file_path"),
                    rs.getString("file_type"),
                    rs.getObject("grade", Double.class),
                    rs.getObject("graded_by", Long.class),
                    rs.getTimestamp("graded_at") != null ? rs.getTimestamp("graded_at").toLocalDateTime() : null,
                    rs.getString("status"),
                    rs.getString("remarks"),
                    rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toLocalDateTime() : null
            );
        }
    };

    public SubmissionRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private void ensureTable() {
        jdbc.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS submissions (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "assignment_id BIGINT," +
                "student_id BIGINT," +
                "content TEXT," +
                "file_data LONGBLOB," +
                "file_type VARCHAR(255)," +
                "file_path TEXT," +
                "grade DOUBLE," +
                "graded_by BIGINT," +
                "graded_at DATETIME," +
                "status VARCHAR(50)," +
                "remarks TEXT," +
                "submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Submission save(Submission s) {
        ensureTable();
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("assignmentId", s.assignmentId())
                .addValue("studentId", s.studentId())
                .addValue("content", s.content())
                .addValue("filePath", s.filePath())
                .addValue("fileType", s.fileType())
                .addValue("grade", s.grade())
                .addValue("gradedBy", s.gradedBy())
                .addValue("gradedAt", s.gradedAt())
                .addValue("status", s.status())
                .addValue("remarks", s.remarks());
        jdbc.update("INSERT INTO submissions (assignment_id, student_id, content, file_path, file_type, grade, graded_by, graded_at, status, remarks, submitted_at) " +
                "VALUES (:assignmentId, :studentId, :content, :filePath, :fileType, :grade, :gradedBy, :gradedAt, :status, :remarks, NOW())", params, kh);
        Long id = kh.getKey() != null ? kh.getKey().longValue() : null;
        return new Submission(id, s.assignmentId(), s.studentId(), s.content(), s.filePath(), s.fileType(), s.grade(), s.gradedBy(), s.gradedAt(), s.status(), s.remarks(), java.time.LocalDateTime.now());
    }

    public Optional<Submission> findById(Long id) {
        ensureTable();
        var params = new MapSqlParameterSource("id", id);
        List<Submission> list = jdbc.query("select * from submissions where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public List<Submission> findByAssignmentId(Long assignmentId) {
        ensureTable();
        var params = new MapSqlParameterSource("assignmentId", assignmentId);
        return jdbc.query("select * from submissions where assignment_id=:assignmentId order by submitted_at desc", params, mapper);
    }

    public Submission grade(Long id, Double grade, Long graderId, String remarks) {
        ensureTable();
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("grade", grade)
                .addValue("graderId", graderId)
                .addValue("remarks", remarks);
        jdbc.update("UPDATE submissions SET grade=:grade, graded_by=:graderId, graded_at=NOW(), status='GRADED', remarks=:remarks WHERE id=:id", params);
        return findById(id).orElse(null);
    }
}
