package com.example.v2.service;

import com.example.v2.model.Course;
import com.example.v2.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    private final CourseRepository repo;

    public CourseService(CourseRepository repo) {
        this.repo = repo;
    }

    public List<Course> listApproved() { return repo.findAllApproved(); }
    public List<Course> listAll() { return repo.findAll(); }
    public List<Course> listPending() { return repo.findPending(); }
    public Optional<Course> find(Integer id) { return repo.findById(id); }
    public List<Course> listByTeacher(Integer teacherId) { return repo.findByTeacher(teacherId); }

    public boolean approve(Integer id, boolean approved){ return repo.updateApproval(id, approved); }
}
