package com.example.v2.controller;

import com.example.v2.model.ClassInfo;
import com.example.v2.model.User;
import com.example.v2.service.StatsService;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/stats")
public class StatsController {
    private final StatsService stats;
    private final UserService users;

    public StatsController(StatsService stats, UserService users) {
        this.stats = stats;
        this.users = users;
    }

    private boolean isAdmin(HttpSession session){ return "ADMIN".equals(session==null?null:session.getAttribute("role")); }
    private boolean isTeacher(HttpSession session){ return "TEACHER".equals(session==null?null:session.getAttribute("role")); }
    private Integer currentUserId(HttpSession session){ Object uid=session==null?null:session.getAttribute("userId"); try{ return uid==null?null:Integer.valueOf(String.valueOf(uid)); }catch(Exception e){ return null; } }

    @GetMapping("/users")
    public Map<String, Object> users(HttpSession session) {
        if(!isAdmin(session)) return Map.of();
        return stats.userCounts();
    }

    @GetMapping("/grade-distribution")
    public List<Map<String, Object>> gradeDistribution(HttpSession session) {
        if(!isAdmin(session)) return List.of();
        return stats.gradeDistribution();
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> classesByGrade(@RequestParam("gradeLevelId") Long gradeLevelId, HttpSession session) {
        if(!isAdmin(session)) return List.of();
        return stats.classesByGrade(gradeLevelId);
    }

    @GetMapping("/grade-scores")
    public List<Map<String, Object>> gradeScores(@RequestParam("gradeLevelId") Long gradeLevelId, HttpSession session) {
        if(!isAdmin(session)) return List.of();
        return stats.gradeScores(gradeLevelId);
    }

    @GetMapping("/class-scores")
    public List<Map<String, Object>> classScores(@RequestParam("classId") Long classId, HttpSession session) {
        if(isAdmin(session)) return stats.classScores(classId);
        if(isTeacher(session)){
            Integer uid=currentUserId(session);
            if(uid!=null){ User u=users.findById(uid).orElse(null); if(u!=null && u.classId()!=null && classId.equals(Long.valueOf(u.classId()))) return stats.classScores(classId); }
        }
        return List.of();
    }

    @GetMapping("/grade-line")
    public List<Map<String, Object>> gradeLine(@RequestParam("gradeLevelId") Long gradeLevelId, HttpSession session) {
        if(!isAdmin(session)) return List.of();
        return stats.gradeLine(gradeLevelId);
    }

    @GetMapping("/class-line")
    public List<Map<String, Object>> classLine(@RequestParam("classId") Long classId, HttpSession session) {
        if(isAdmin(session)) return stats.classLine(classId);
        if(isTeacher(session)){
            Integer uid=currentUserId(session);
            if(uid!=null){ User u=users.findById(uid).orElse(null); if(u!=null && u.classId()!=null && classId.equals(Long.valueOf(u.classId()))) return stats.classLine(classId); }
        }
        return List.of();
    }

    @GetMapping("/classes-by-grade")
    public List<ClassInfo> classesByGradeEntities(@RequestParam("gradeLevelId") Long gradeLevelId, HttpSession session) {
        if(!isAdmin(session)) return List.of();
        return stats.classesByGradeEntities(gradeLevelId);
    }

    @GetMapping("/grade-score-series")
    public Map<String, Object> gradeScoreSeries(HttpSession session) {
        if(!isAdmin(session)) return Map.of();
        return stats.gradeScoreSeries();
    }

    @GetMapping("/grade-subject-line")
    public Map<String, Object> gradeSubjectLine(@RequestParam("gradeLevelId") Long gradeLevelId, HttpSession session) {
        if(!isAdmin(session)) return Map.of();
        return stats.gradeSubjectLine(gradeLevelId);
    }

    @GetMapping("/class-subject-line")
    public Map<String, Object> classSubjectLine(@RequestParam("classId") Long classId, HttpSession session) {
        if(isAdmin(session)) return stats.classSubjectLine(classId);
        if(isTeacher(session)){
            Integer uid=currentUserId(session);
            if(uid!=null){ User u=users.findById(uid).orElse(null); if(u!=null && u.classId()!=null && classId.equals(Long.valueOf(u.classId()))) return stats.classSubjectLine(classId); }
        }
        return Map.of();
    }

    @GetMapping("/class-score-series")
    public Map<String, Object> classScoreSeries(@RequestParam(value = "gradeLevelId", required = false) Long gradeLevelId,
                                               @RequestParam(value = "classId", required = false) Long classId,
                                               HttpSession session) {
        if(isAdmin(session)) return stats.classScoreSeries(gradeLevelId);
        if(isTeacher(session)){
            Integer uid=currentUserId(session);
            if(uid!=null){ User u=users.findById(uid).orElse(null); if(u!=null && u.classId()!=null){
                if(classId!=null && !classId.equals(Long.valueOf(u.classId()))) return Map.of();
                return stats.classScoreSeries(Long.valueOf(u.classId()));
            }}
        }
        return Map.of();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
