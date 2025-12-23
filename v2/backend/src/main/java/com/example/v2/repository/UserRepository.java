package com.example.v2.repository;

import com.example.v2.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<User> mapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getObject("class_id", Integer.class),
                    rs.getString("student_number")
            );
        }
    };

        record AuthUser(
            Integer id,
            String username,
            String role,
            Integer classId,
            String studentNumber,
            String password
        ) {}

    private final RowMapper<AuthUser> authMapper = new RowMapper<AuthUser>() {
        @Override
        public AuthUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AuthUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getObject("class_id", Integer.class),
                    rs.getString("student_number"),
                    rs.getString("password")
            );
        }
    };

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        var params = new MapSqlParameterSource("username", username);
        List<User> list = jdbc.query("select id,username,role,class_id,student_number from users where username=:username", params, mapper);
        return list.stream().findFirst();
    }

    public Optional<User> findById(Integer id) {
        var params = new MapSqlParameterSource("id", id);
        List<User> list = jdbc.query("select id,username,role,class_id,student_number from users where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public Optional<AuthUser> findAuthByUsername(String username) {
        var params = new MapSqlParameterSource("username", username);
        List<AuthUser> list = jdbc.query("select id,username,role,class_id,student_number,password from users where username=:username", params, authMapper);
        return list.stream().findFirst();
    }

    public Optional<User> authenticate(String username, String password){
        return findAuthByUsername(username)
                .filter(u -> password != null && password.equals(u.password()))
                .map(u -> new User(u.id(), u.username(), u.role(), u.classId(), u.studentNumber()));
    }

    public List<User> findTeachers() {
        return jdbc.query("select id,username,role,class_id,student_number from users where role='TEACHER'", mapper);
    }

    public List<User> findAll() {
        return jdbc.query("select id,username,role,class_id,student_number from users order by id desc", mapper);
    }

    public Optional<User> create(String username, String password, String role, Integer classId, String studentNumber){
        var params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("password", password)
                .addValue("role", role)
                .addValue("classId", classId)
                .addValue("studentNumber", studentNumber);
        var sql = "insert into users(username,password,role,class_id,student_number) " +
                "values(:username,:password,:role,:classId,:studentNumber)";
        var kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
        int updated = jdbc.update(sql, params, kh, new String[]{"id"});
        if(updated <= 0) return Optional.empty();
        Number key = kh.getKey();
        if(key == null) return Optional.empty();
        return findById(key.intValue());
    }

    public boolean updatePassword(Integer id, String newPassword){
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("password", newPassword);
        return jdbc.update("update users set password=:password where id=:id", params) > 0;
    }

    public boolean updateUser(Integer id, String username, String role, Integer classId, String studentNumber){
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("username", username)
                .addValue("role", role)
                .addValue("classId", classId)
                .addValue("studentNumber", studentNumber);
        String sql = "update users set " +
                "username=COALESCE(:username, username), " +
                "role=COALESCE(:role, role), " +
                "class_id=:classId, " +
                "student_number=:studentNumber " +
                "where id=:id";
        return jdbc.update(sql, params) > 0;
    }
}
