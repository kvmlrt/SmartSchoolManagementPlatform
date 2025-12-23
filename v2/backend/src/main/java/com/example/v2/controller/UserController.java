package com.example.v2.controller;

import com.example.v2.model.User;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping("/teachers")
    public List<User> teachers(){
        return users.listTeachers();
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
