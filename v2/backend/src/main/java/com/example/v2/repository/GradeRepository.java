package com.example.v2.repository;

import com.example.v2.model.Grade;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class GradeRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Grade> mapper = new RowMapper<>() {
        @Override
        public Grade mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Grade(
                    rs.getInt("id"),
                    rs.getObject("student_id", Integer.class),
                    rs.getObject("course_id", Integer.class),
                    rs.getObject("score", Double.class),
                    rs.getObject("modified_by", Integer.class),
                    rs.getTimestamp("modified_at") != null ? rs.getTimestamp("modified_at").toLocalDateTime() : null,
                    rs.getObject("class_id", Long.class),
                    rs.getString("type"),
                    rs.getString("remarks")
            );
        }
    };

    public GradeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Grade> findAll() {
        return jdbc.query("select * from grades order by id desc", mapper);
    }

    public List<Grade> findByClass(Integer classId) {
        var params = new MapSqlParameterSource("classId", classId);
        return jdbc.query("select * from grades where class_id=:classId order by id desc", params, mapper);
    }

    public List<Grade> findByStudent(Integer studentId) {
        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query("select * from grades where student_id=:studentId order by id desc", params, mapper);
    }

    public List<Grade> findByStudentNumber(String studentNumber) {
        var params = new MapSqlParameterSource("studentNumber", studentNumber);
        String sql = "select g.* from grades g join users u on g.student_id = u.id where u.student_number=:studentNumber order by g.id desc";
        return jdbc.query(sql, params, mapper);
    }

    public Optional<Grade> findById(Integer id) {
        var params = new MapSqlParameterSource("id", id);
        List<Grade> list = jdbc.query("select * from grades where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public Grade save(Grade g) {
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("studentId", g.studentId())
                .addValue("courseId", g.courseId())
                .addValue("score", g.score())
                .addValue("modifiedBy", g.modifiedBy())
                .addValue("classId", g.classId())
                .addValue("type", g.type())
                .addValue("remarks", g.remarks());
        jdbc.update("INSERT INTO grades (student_id, course_id, score, modified_by, modified_at, class_id, type, remarks) " +
                "VALUES (:studentId, :courseId, :score, :modifiedBy, NOW(), :classId, :type, :remarks)", params, kh);
        Integer id = kh.getKey() != null ? kh.getKey().intValue() : null;
        return new Grade(id, g.studentId(), g.courseId(), g.score(), g.modifiedBy(), LocalDateTime.now(), g.classId(), g.type(), g.remarks());
    }

    public Grade update(Integer id, Grade g) {
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("studentId", g.studentId())
                .addValue("courseId", g.courseId())
                .addValue("score", g.score())
                .addValue("modifiedBy", g.modifiedBy())
                .addValue("classId", g.classId())
                .addValue("type", g.type())
                .addValue("remarks", g.remarks());
        jdbc.update("UPDATE grades SET student_id=:studentId, course_id=:courseId, score=:score, modified_by=:modifiedBy, modified_at=NOW(), class_id=:classId, type=:type, remarks=:remarks WHERE id=:id", params);
        return findById(id).orElse(null);
    }

    public void delete(Integer id) {
        var params = new MapSqlParameterSource("id", id);
        jdbc.update("DELETE FROM grades WHERE id=:id", params);
    }
}
