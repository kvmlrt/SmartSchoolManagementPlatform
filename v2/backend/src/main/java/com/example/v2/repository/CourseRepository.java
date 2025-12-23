package com.example.v2.repository;

import com.example.v2.model.Course;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CourseRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Course> mapper = new RowMapper<Course>() {
        @Override
        public Course mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Course(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getObject("teacher_id", Integer.class),
                    rs.getObject("approved", Boolean.class),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
            );
        }
    };

    public CourseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Course> findAllApproved() {
        return jdbc.query("select id,title,description,teacher_id,approved,created_at from courses where approved = 1", mapper);
    }

    public List<Course> findAll() {
        return jdbc.query("select id,title,description,teacher_id,approved,created_at from courses", mapper);
    }

    public List<Course> findPending() {
        return jdbc.query("select id,title,description,teacher_id,approved,created_at from courses where approved is null or approved = 0", mapper);
    }

    public Optional<Course> findById(Integer id) {
        var params = new MapSqlParameterSource("id", id);
        List<Course> list = jdbc.query("select id,title,description,teacher_id,approved,created_at from courses where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public List<Course> findByTeacher(Integer teacherId) {
        var params = new MapSqlParameterSource("teacherId", teacherId);
        return jdbc.query("select id,title,description,teacher_id,approved,created_at from courses where teacher_id=:teacherId", params, mapper);
    }

    public boolean updateApproval(Integer id, boolean approved) {
        var params = new MapSqlParameterSource("id", id).addValue("approved", approved);
        int updated = jdbc.update("update courses set approved=:approved where id=:id", params);
        return updated > 0;
    }
}
