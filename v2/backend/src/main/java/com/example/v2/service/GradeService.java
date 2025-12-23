package com.example.v2.service;

import com.example.v2.model.Grade;
import com.example.v2.repository.GradeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeService {
    private final GradeRepository repo;
    public GradeService(GradeRepository repo) { this.repo = repo; }
    public List<Grade> listAll(){ return repo.findAll(); }
    public List<Grade> listByClass(Integer classId){ return repo.findByClass(classId); }
    public List<Grade> listByStudent(Integer studentId){ return repo.findByStudent(studentId); }
    public List<Grade> listByStudentNumber(String studentNumber){ return repo.findByStudentNumber(studentNumber); }
    public Grade save(Grade g){ return repo.save(g); }
    public Grade update(Integer id, Grade g){ return repo.update(id, g); }
    public void delete(Integer id){ repo.delete(id); }
}
