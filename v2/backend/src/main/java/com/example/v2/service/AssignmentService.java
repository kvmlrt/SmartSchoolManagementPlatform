package com.example.v2.service;

import com.example.v2.model.Assignment;
import com.example.v2.model.Submission;
import com.example.v2.repository.AssignmentRepository;
import com.example.v2.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;

    public AssignmentService(AssignmentRepository assignments, SubmissionRepository submissions) {
        this.assignments = assignments;
        this.submissions = submissions;
    }

    public Assignment create(Assignment a){ return assignments.save(a); }
    public List<Assignment> listAll(){ return assignments.findAll(); }
    public List<Assignment> listByTeacher(Long teacherId){ return assignments.findByTeacherId(teacherId); }
    public List<Assignment> listByClass(Long classId){ return assignments.findByClassId(classId); }
    public Optional<Assignment> find(Long id){ return assignments.findById(id); }

    public Submission submit(Submission s){ return submissions.save(s); }
    public List<Submission> listSubmissions(Long assignmentId){ return submissions.findByAssignmentId(assignmentId); }
    public Optional<Submission> findSubmission(Long id){ return submissions.findById(id); }
    public Submission gradeSubmission(Long id, Double grade, Long graderId, String remarks){ return submissions.grade(id, grade, graderId, remarks); }
}
