package com.example.v2.repository;

import com.example.v2.model.Resource;
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
public class ResourceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Resource> mapper = new RowMapper<Resource>() {
        @Override
        public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Resource(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("url"),
                    rs.getBytes("data"),
                    rs.getString("content_type"),
                    rs.getTimestamp("uploaded_at") != null ? rs.getTimestamp("uploaded_at").toLocalDateTime() : null,
                    rs.getString("file_path")
            );
        }
    };

    public ResourceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void ensureTable() {
        jdbc.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS resources (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(255)," +
                "url VARCHAR(512)," +
                "data LONGBLOB," +
                "content_type VARCHAR(255)," +
                "uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "file_path TEXT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public List<Resource> findAll() {
        ensureTable();
        return jdbc.query("select * from resources order by uploaded_at desc", mapper);
    }

    public Optional<Resource> findById(Long id) {
        ensureTable();
        var params = new MapSqlParameterSource("id", id);
        List<Resource> list = jdbc.query("select * from resources where id=:id", params, mapper);
        return list.stream().findFirst();
    }

    public Resource save(Resource r) {
        ensureTable();
        var kh = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("name", r.name())
                .addValue("url", r.url())
                .addValue("data", r.data())
                .addValue("contentType", r.contentType())
                .addValue("filePath", r.filePath());
        jdbc.update("INSERT INTO resources (name, url, data, content_type, uploaded_at, file_path) VALUES (:name, :url, :data, :contentType, NOW(), :filePath)", params, kh);
        Long id = kh.getKey() != null ? kh.getKey().longValue() : null;
        return new Resource(id, r.name(), r.url(), r.data(), r.contentType(), LocalDateTime.now(), r.filePath());
    }

    public void delete(Long id) {
        ensureTable();
        var params = new MapSqlParameterSource("id", id);
        jdbc.update("DELETE FROM resources WHERE id=:id", params);
    }
}
