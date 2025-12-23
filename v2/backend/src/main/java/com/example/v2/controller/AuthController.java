package com.example.v2.controller;

import com.example.v2.model.User;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/auth")
public class AuthController {
    private final UserService users;
    public AuthController(UserService users) { this.users = users; }

    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestParam String username,
                                                    @RequestParam String password,
                                                    HttpSession session){
        Optional<User> u = users.authenticate(username, password);
        if (u.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "用户名或密码错误"));
        }
        User user = u.get();
        try { session.setAttribute("userId", user.id()); } catch (Exception ignored) {}
        try { session.setAttribute("role", user.role()); } catch (Exception ignored) {}
        Map<String,Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("id", user.id());
        res.put("username", user.username());
        res.put("role", user.role());
        res.put("classId", user.classId());
        res.put("studentNumber", user.studentNumber());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String,Object>> logout(HttpSession session){
        try { session.invalidate(); } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public Map<String,Object> me(HttpSession session){
        Object uid = session == null ? null : session.getAttribute("userId");
        Map<String,Object> res = new HashMap<>();
        if (uid == null) { res.put("loggedIn", false); return res; }
        Integer id = null;
        try { id = Integer.valueOf(String.valueOf(uid)); } catch (Exception ignored) {}
        if (id == null) { res.put("loggedIn", false); return res; }
        Optional<User> u = users.findById(id);
        if (u.isEmpty()) { res.put("loggedIn", false); return res; }
        User user = u.get();
        res.put("loggedIn", true);
        res.put("id", user.id());
        res.put("username", user.username());
        res.put("role", user.role());
        res.put("classId", user.classId());
        res.put("studentNumber", user.studentNumber());
        return res;
    }
}
