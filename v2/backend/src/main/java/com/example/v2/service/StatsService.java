package com.example.v2.service;

import com.example.v2.model.ClassInfo;
import com.example.v2.model.GradeLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

@Service
public class StatsService {
    private final NamedParameterJdbcTemplate jdbc;
    private record CourseLabel(Integer id, String title) {}

    public StatsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> userCounts() {
        Long total = jdbc.getJdbcTemplate().queryForObject("select count(*) from users", Long.class);
        Long students = jdbc.getJdbcTemplate().queryForObject("select count(*) from users where role='STUDENT'", Long.class);
        Map<String, Object> res = new HashMap<>();
        res.put("totalUsers", total != null ? total : 0L);
        res.put("totalStudents", students != null ? students : 0L);
        return res;
    }

    public List<Map<String, Object>> gradeDistribution() {
        String sql = """
                SELECT gl.id, gl.name, COUNT(u.id) AS cnt
                FROM grade_levels gl
                LEFT JOIN classes c ON c.grade_level_id = gl.id
                LEFT JOIN users u ON u.class_id = c.id
                GROUP BY gl.id, gl.name
                ORDER BY gl.id
                """;
        return jdbc.query(sql, (rs, i) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("grade", rs.getString("name"));
            m.put("count", rs.getLong("cnt"));
            return m;
        });
    }

    public List<Map<String, Object>> classesByGrade(Long gradeLevelId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ClassInfo c : getClassesByGradeLevel(gradeLevelId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.id());
            m.put("name", c.name());
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> gradeScores(Long gradeLevelId) {
        return averageScoresByDate(classIdsForGrade(gradeLevelId));
    }

    public List<Map<String, Object>> classScores(Long classId) {
        return averageScoresByDate(classId == null ? List.of() : List.of(classId));
    }

    public List<Map<String, Object>> gradeLine(Long gradeLevelId) {
        return averageScoresByDate(classIdsForGrade(gradeLevelId));
    }

    public List<Map<String, Object>> classLine(Long classId) {
        return averageScoresByDate(classId == null ? List.of() : List.of(classId));
    }

    public List<ClassInfo> classesByGradeEntities(Long gradeLevelId) {
        return getClassesByGradeLevel(gradeLevelId);
    }

    public Map<String, Object> gradeScoreSeries() {
        List<CourseLabel> courses = courses();
        List<GradeLevel> grades = gradeLevels();
        if (courses.isEmpty() || grades.isEmpty()) return Map.of();

        String sql = """
                SELECT cls.grade_level_id AS grade_id, g.course_id AS course_id, AVG(g.score) AS avg_score
                FROM grades g
                JOIN classes cls ON cls.id = g.class_id
                WHERE g.score IS NOT NULL
                GROUP BY cls.grade_level_id, g.course_id
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource());
        Map<Long, Map<Integer, Double>> grid = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object gidRaw = r.get("grade_id");
            Object cidRaw = r.get("course_id");
            if (gidRaw == null || cidRaw == null) continue;
            Long gid = ((Number) gidRaw).longValue();
            Integer cid = ((Number) cidRaw).intValue();
            Double avg = r.get("avg_score") != null ? ((Number) r.get("avg_score")).doubleValue() : null;
            grid.computeIfAbsent(gid, k -> new HashMap<>()).put(cid, avg);
        }

        List<String> labels = courses.stream().map(CourseLabel::title).toList();
        List<Map<String, Object>> series = new ArrayList<>();
        for (GradeLevel gl : grades) {
            Map<Integer, Double> courseMap = grid.getOrDefault(gl.id(), Map.of());
            List<Double> data = new ArrayList<>();
            for (CourseLabel c : courses) {
                data.add(courseMap.get(c.id()));
            }
            Map<String, Object> s = new HashMap<>();
            s.put("name", gl.name());
            s.put("data", data);
            series.add(s);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("labels", labels);
        res.put("series", series);
        return res;
    }

    public Map<String, Object> gradeSubjectLine(Long gradeLevelId) {
        if (gradeLevelId == null) return Map.of();
        List<CourseLabel> courses = courses();
        if (courses.isEmpty()) return Map.of();

        String sql = """
                SELECT g.course_id AS course_id, DATE(g.modified_at) AS dt, AVG(g.score) AS avg_score
                FROM grades g
                JOIN classes cls ON cls.id = g.class_id
                WHERE cls.grade_level_id = :gradeLevelId AND g.modified_at IS NOT NULL
                GROUP BY g.course_id, DATE(g.modified_at)
                ORDER BY dt
                """;
        var params = new MapSqlParameterSource("gradeLevelId", gradeLevelId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);

        TreeSet<String> labelsSet = new TreeSet<>();
        Map<Integer, Map<String, Double>> dataMap = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object cidRaw = r.get("course_id");
            Object dtRaw = r.get("dt");
            if (cidRaw == null || dtRaw == null) continue;
            Integer cid = ((Number) cidRaw).intValue();
            String date = dtRaw.toString();
            Double avg = r.get("avg_score") != null ? ((Number) r.get("avg_score")).doubleValue() : null;
            labelsSet.add(date);
            dataMap.computeIfAbsent(cid, k -> new HashMap<>()).put(date, avg);
        }

        List<String> labels = new ArrayList<>(labelsSet);
        List<Map<String, Object>> series = new ArrayList<>();
        for (CourseLabel c : courses) {
            Map<String, Double> values = dataMap.getOrDefault(c.id(), Map.of());
            List<Double> data = new ArrayList<>();
            for (String d : labels) {
                data.add(values.get(d));
            }
            Map<String, Object> m = new HashMap<>();
            m.put("name", c.title());
            m.put("data", data);
            series.add(m);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("labels", labels);
        res.put("series", series);
        return res;
    }

    public Map<String, Object> classSubjectLine(Long classId) {
        if (classId == null) return Map.of();
        List<CourseLabel> courses = courses();
        if (courses.isEmpty()) return Map.of();

        String sql = """
                SELECT g.course_id AS course_id, DATE(g.modified_at) AS dt, AVG(g.score) AS avg_score
                FROM grades g
                WHERE g.class_id = :classId AND g.modified_at IS NOT NULL
                GROUP BY g.course_id, DATE(g.modified_at)
                ORDER BY dt
                """;
        var params = new MapSqlParameterSource("classId", classId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);

        TreeSet<String> labelsSet = new TreeSet<>();
        Map<Integer, Map<String, Double>> dataMap = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object cidRaw = r.get("course_id");
            Object dtRaw = r.get("dt");
            if (cidRaw == null || dtRaw == null) continue;
            Integer cid = ((Number) cidRaw).intValue();
            String date = dtRaw.toString();
            Double avg = r.get("avg_score") != null ? ((Number) r.get("avg_score")).doubleValue() : null;
            labelsSet.add(date);
            dataMap.computeIfAbsent(cid, k -> new HashMap<>()).put(date, avg);
        }

        List<String> labels = new ArrayList<>(labelsSet);
        List<Map<String, Object>> series = new ArrayList<>();
        for (CourseLabel c : courses) {
            Map<String, Double> values = dataMap.getOrDefault(c.id(), Map.of());
            List<Double> data = new ArrayList<>();
            for (String d : labels) {
                data.add(values.get(d));
            }
            Map<String, Object> m = new HashMap<>();
            m.put("name", c.title());
            m.put("data", data);
            series.add(m);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("labels", labels);
        res.put("series", series);
        return res;
    }

    public Map<String, Object> classScoreSeries(Long gradeLevelId) {
        List<CourseLabel> courses = courses();
        if (courses.isEmpty()) return Map.of();

        Long resolvedGradeId = gradeLevelId;
        if (resolvedGradeId == null) {
            Optional<GradeLevel> first = gradeLevels().stream().findFirst();
            if (first.isEmpty()) return Map.of();
            resolvedGradeId = first.get().id();
        }

        List<ClassInfo> classes = getClassesByGradeLevel(resolvedGradeId);
        if (classes.isEmpty()) return Map.of("labels", courses.stream().map(CourseLabel::title).toList(), "series", List.of());

        List<Long> classIds = classes.stream().map(ClassInfo::id).toList();
        var params = new MapSqlParameterSource("classIds", classIds);
        String sql = """
                SELECT g.class_id AS class_id, g.course_id AS course_id, AVG(g.score) AS avg_score
                FROM grades g
                WHERE g.class_id IN (:classIds) AND g.score IS NOT NULL
                GROUP BY g.class_id, g.course_id
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        Map<Long, Map<Integer, Double>> grid = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object cidRaw = r.get("class_id");
            Object courseRaw = r.get("course_id");
            if (cidRaw == null || courseRaw == null) continue;
            Long cid = ((Number) cidRaw).longValue();
            Integer courseId = ((Number) courseRaw).intValue();
            Double avg = r.get("avg_score") != null ? ((Number) r.get("avg_score")).doubleValue() : null;
            grid.computeIfAbsent(cid, k -> new HashMap<>()).put(courseId, avg);
        }

        List<String> labels = courses.stream().map(CourseLabel::title).toList();
        List<Map<String, Object>> series = new ArrayList<>();
        for (ClassInfo cl : classes) {
            Map<Integer, Double> values = grid.getOrDefault(cl.id(), Map.of());
            List<Double> data = new ArrayList<>();
            for (CourseLabel c : courses) {
                data.add(values.get(c.id()));
            }
            Map<String, Object> m = new HashMap<>();
            m.put("name", cl.name());
            m.put("data", data);
            series.add(m);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("labels", labels);
        res.put("series", series);
        return res;
    }

    private List<Map<String, Object>> averageScoresByDate(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) return List.of();
        var params = new MapSqlParameterSource("classIds", classIds);
        String sql = "SELECT DATE(modified_at) AS dt, AVG(score) AS avg_score FROM grades WHERE class_id IN (:classIds) AND modified_at IS NOT NULL GROUP BY DATE(modified_at) ORDER BY dt";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", r.get("dt") != null ? r.get("dt").toString() : null);
            m.put("avg", r.get("avg_score") != null ? ((Number) r.get("avg_score")).doubleValue() : 0.0);
            out.add(m);
        }

        String unknownSql = "SELECT AVG(score) AS avg_score FROM grades WHERE class_id IN (:classIds) AND modified_at IS NULL";
        Double unknownAvg = jdbc.queryForObject(unknownSql, params, Double.class);
        if (unknownAvg != null) {
            Map<String, Object> unknown = new HashMap<>();
            unknown.put("date", "unknown");
            unknown.put("avg", unknownAvg);
            out.add(unknown);
        }
        return out;
    }

    private List<Long> classIdsForGrade(Long gradeLevelId) {
        if (gradeLevelId == null) return List.of();
        var params = new MapSqlParameterSource("gradeLevelId", gradeLevelId);
        String sql = "SELECT id FROM classes WHERE grade_level_id = :gradeLevelId ORDER BY id";
        return jdbc.query(sql, params, (rs, i) -> rs.getLong("id"));
    }

    private List<GradeLevel> gradeLevels() {
        String sql = "SELECT id, name, parent_id FROM grade_levels ORDER BY id";
        return jdbc.query(sql, (rs, i) -> new GradeLevel(
                rs.getObject("id", Long.class),
                rs.getString("name"),
                rs.getObject("parent_id", Long.class)
        ));
    }

    private List<CourseLabel> courses() {
        String sql = "SELECT id, title FROM courses ORDER BY id";
        return jdbc.query(sql, (rs, i) -> new CourseLabel(
                rs.getObject("id", Integer.class),
                rs.getString("title")
        ));
    }

    private List<ClassInfo> getClassesByGradeLevel(Long gradeLevelId) {
        if (gradeLevelId == null) return List.of();
        var params = new MapSqlParameterSource("gradeLevelId", gradeLevelId);
        String sql = "SELECT id, name FROM classes WHERE grade_level_id = :gradeLevelId ORDER BY id";
        return jdbc.query(sql, params, (rs, i) -> new ClassInfo(
                rs.getObject("id", Long.class),
                rs.getString("name"),
                gradeLevelId
        ));
    }
}
