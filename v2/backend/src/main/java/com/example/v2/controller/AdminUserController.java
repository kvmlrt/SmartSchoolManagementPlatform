package com.example.v2.controller;

import com.example.v2.model.User;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/admin/users")
public class AdminUserController {
    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    private boolean isAdmin(HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        return "ADMIN".equals(role);
    }

    @GetMapping
    public ResponseEntity<List<User>> list(HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(users.listAll());
    }

    public record CreateUserRequest(String username, String password, String role, Integer classId, String studentNumber){}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserRequest req, HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).body("forbidden");
        if(req == null || !StringUtils.hasText(req.username()) || !StringUtils.hasText(req.password())){
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码必填"));
        }
        String role = StringUtils.hasText(req.role()) ? req.role().toUpperCase() : "STUDENT";
        Optional<User> created = users.create(req.username(), req.password(), role, req.classId(), req.studentNumber());
        if(created.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "创建失败，可能是用户名重复"));
        return ResponseEntity.ok(created.get());
    }

    public record UpdatePasswordRequest(String password){}

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Integer id, @RequestBody UpdatePasswordRequest req, HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).body("forbidden");
        if(req == null || !StringUtils.hasText(req.password())){
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
        }
        boolean ok = users.updatePassword(id, req.password());
        if(!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    public record UpdateUserRequest(String username, String role, Integer classId, String studentNumber){}

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody UpdateUserRequest req, HttpSession session){
        if(!isAdmin(session)) return ResponseEntity.status(403).body("forbidden");
        if(req == null) return ResponseEntity.badRequest().body(Map.of("error", "请求无内容"));
        String role = StringUtils.hasText(req.role()) ? req.role().toUpperCase() : null;
        boolean ok = users.updateUser(id, req.username(), role, req.classId(), req.studentNumber());
        if(!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
