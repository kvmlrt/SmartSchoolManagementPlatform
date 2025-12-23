package com.example.v2.controller;

import com.example.v2.model.Grade;
import com.example.v2.model.User;
import com.example.v2.service.GradeService;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/grades")
public class GradeController {
    private final GradeService grades;
    private final UserService users;
    public GradeController(GradeService grades, UserService users) { this.grades = grades; this.users = users; }

    @GetMapping
    public List<Grade> list(@RequestParam(name = "classId", required = false) Integer classId,
                            @RequestParam(name = "studentId", required = false) Integer studentId,
                            @RequestParam(name = "studentNumber", required = false) String studentNumber,
                            HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        Object uidObj = session == null ? null : session.getAttribute("userId");
        Integer uid = null;
        try { if (uidObj != null) uid = Integer.valueOf(String.valueOf(uidObj)); } catch (Exception ignored) {}
        if (uid == null || role == null) return List.of();

        boolean isAdmin = "ADMIN".equals(role);
        boolean isStudent = "STUDENT".equals(role);
        boolean isTeacher = "TEACHER".equals(role);

        if (isStudent) {
            // 学生只能看自己的成绩，忽略传入参数
            return grades.listByStudent(uid);
        }

        if (isTeacher) {
            // 教师只能看自己班级的成绩
            User u = users.findById(uid).orElse(null);
            if (u == null || u.classId() == null) return List.of();
            return grades.listByClass(u.classId());
        }

        // 管理员保持原有查询能力
        if (studentNumber != null) return grades.listByStudentNumber(studentNumber);
        if (studentId != null) return grades.listByStudent(studentId);
        if (classId != null) return grades.listByClass(classId);
        return grades.listAll();
    }

    @PostMapping
    public ResponseEntity<Grade> create(@RequestBody Grade g, HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(grades.save(g));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> update(@PathVariable Integer id, @RequestBody Grade g, HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        Grade updated = grades.update(id, g);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        grades.delete(id);
        return ResponseEntity.ok().build();
    }
}
