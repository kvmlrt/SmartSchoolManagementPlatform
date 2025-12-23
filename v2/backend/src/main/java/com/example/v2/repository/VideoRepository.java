package com.example.v2.repository;

import com.example.v2.model.Video;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VideoRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Video> mapper = new RowMapper<Video>() {
        @Override
        public Video mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Video(
                    rs.getLong("id"),
                    rs.getObject("course_id", Integer.class),
                    rs.getObject("teacher_id", Integer.class),
                    rs.getString("filename"),
                    rs.getString("path"),
                    rs.getString("thumbnail_path"),
                    rs.getString("title"),
                    rs.getString("content_type"),
                    rs.getObject("size", Long.class),
                    rs.getTimestamp("uploaded_at") != null ? rs.getTimestamp("uploaded_at").toLocalDateTime() : null,
                    rs.getObject("approved", Integer.class),
                    rs.getString("review_remark"),
                    rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null
            );
        }
    };

    public VideoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Video> findByTeacher(Integer teacherId) {
        var params = new MapSqlParameterSource("teacherId", teacherId);
        return jdbc.query("select * from course_videos where teacher_id=:teacherId order by uploaded_at desc", params, mapper);
    }

    public List<Video> findByTeacherPublic(Integer teacherId) {
        var params = new MapSqlParameterSource("teacherId", teacherId);
        return jdbc.query("select * from course_videos where teacher_id=:teacherId and approved=1 order by uploaded_at desc", params, mapper);
    }

    public List<Video> findByCourse(Integer courseId) {
        var params = new MapSqlParameterSource("courseId", courseId);
        return jdbc.query("select * from course_videos where course_id=:courseId order by uploaded_at desc", params, mapper);
    }

    public Video findById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        List<Video> list = jdbc.query("select * from course_videos where id=:id", params, mapper);
        return list.stream().findFirst().orElse(null);
    }

    public void save(Integer courseId,
                     Integer teacherId,
                     String filename,
                     String path,
                     String thumbnailPath,
                     String title,
                     String contentType,
                     Long size) {
        var params = new MapSqlParameterSource()
                .addValue("courseId", courseId)
                .addValue("teacherId", teacherId)
                .addValue("filename", filename)
                .addValue("path", path)
                .addValue("thumbnailPath", thumbnailPath)
                .addValue("title", title)
                .addValue("contentType", contentType)
                .addValue("size", size);
        jdbc.update("INSERT INTO course_videos (course_id, teacher_id, filename, path, thumbnail_path, title, content_type, size, uploaded_at, approved) " +
            "VALUES (:courseId, :teacherId, :filename, :path, :thumbnailPath, :title, :contentType, :size, NOW(), 0)", params);
    }

    public void deleteById(Long id) {
        var params = new MapSqlParameterSource("id", id);
        jdbc.update("DELETE FROM course_videos WHERE id=:id", params);
    }

    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        var params = new MapSqlParameterSource("ids", ids);
        jdbc.update("DELETE FROM course_videos WHERE id IN (:ids)", params);
    }

    public List<Video> findPending(){
        return jdbc.query("select * from course_videos where approved=0 order by uploaded_at desc", new MapSqlParameterSource(), mapper);
    }

    public void setApproved(Long id, int approved, String remark){
        var params=new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("approved", approved)
                .addValue("remark", remark);
        jdbc.update("update course_videos set approved=:approved, review_remark=:remark, reviewed_at=NOW() where id=:id", params);
    }
}
