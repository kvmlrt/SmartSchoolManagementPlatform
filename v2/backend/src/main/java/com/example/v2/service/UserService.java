package com.example.v2.service;

import com.example.v2.model.User;
import com.example.v2.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }
    public Optional<User> findByUsername(String username) { return repo.findByUsername(username); }
    public Optional<User> findById(Integer id) { return repo.findById(id); }
    public List<User> listTeachers(){ return repo.findTeachers(); }
    public List<User> listAll(){ return repo.findAll(); }

    public Optional<User> create(String username, String password, String role, Integer classId, String studentNumber){
        return repo.create(username, password, role, classId, studentNumber);
    }

    public boolean updatePassword(Integer id, String newPassword){
        return repo.updatePassword(id, newPassword);
    }

    public boolean updateUser(Integer id, String username, String role, Integer classId, String studentNumber){
        return repo.updateUser(id, username, role, classId, studentNumber);
    }

    public Optional<User> authenticate(String username, String password){
        return repo.authenticate(username, password);
    }
}
