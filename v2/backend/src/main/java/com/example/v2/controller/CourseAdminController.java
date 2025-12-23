package com.example.v2.controller;

import com.example.v2.model.Course;
import com.example.v2.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/courses")
public class CourseAdminController {
    private final CourseService courses;

    public CourseAdminController(CourseService courses) {
        this.courses = courses;
    }

    private boolean isAdmin(HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        return "ADMIN".equals(role);
    }

    @GetMapping
    public ResponseEntity<List<Course>> list(@RequestParam(name = "pending", required = false) Boolean pending,
                                             HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).build();
        if(Boolean.TRUE.equals(pending)) return ResponseEntity.ok(courses.listPending());
        return ResponseEntity.ok(courses.listAll());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer id,
                                     @RequestParam(name = "approved", defaultValue = "true") boolean approved,
                                     HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).body("forbidden");
        boolean ok = courses.approve(id, approved);
        if(!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }
}
