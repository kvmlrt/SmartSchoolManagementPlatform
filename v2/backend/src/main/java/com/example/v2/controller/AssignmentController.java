package com.example.v2.controller;

import com.example.v2.model.Assignment;
import com.example.v2.model.Submission;
import com.example.v2.service.AssignmentService;
import com.example.v2.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/assignments")
public class AssignmentController {
    private final AssignmentService assignments;
    private final UserService users;

    public AssignmentController(AssignmentService assignments, UserService users) {
        this.assignments = assignments;
        this.users = users;
    }

    @GetMapping
    public List<Assignment> list(@RequestParam(name = "classId", required = false) Long classId,
                                 @RequestParam(name = "teacherId", required = false) Long teacherId){
        if (classId != null) return assignments.listByClass(classId);
        if (teacherId != null) return assignments.listByTeacher(teacherId);
        return assignments.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> get(@PathVariable Long id){
        return assignments.find(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<?> info(@PathVariable Long id){
        Optional<Assignment> a = assignments.find(id);
        if (a.isEmpty()) return ResponseEntity.notFound().build();
        Map<String,Object> payload = Map.of(
                "assignment", a.get(),
                "submissions", assignments.listSubmissions(id)
        );
        return ResponseEntity.ok(payload);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Assignment a, HttpSession session){
        Object uid = session == null ? null : session.getAttribute("userId");
        Object role = session == null ? null : session.getAttribute("role");
        if(uid == null || role == null || (!"TEACHER".equals(role) && !"ADMIN".equals(role))){
            return ResponseEntity.status(403).body(Map.of("error","需教师/管理员登录"));
        }
        Long creator = null;
        try { creator = Long.valueOf(String.valueOf(uid)); } catch (Exception ignored) {}
        Assignment toSave = new Assignment(null, a.title(), a.description(), a.courseId(), a.dueDate(), creator, a.createdAt());
        Assignment saved = assignments.create(toSave);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id,
                                    @RequestParam(name = "studentId", required = false) Long studentId,
                                    @RequestParam(name = "content", required = false) String content,
                                    @RequestParam(name = "file", required = false) MultipartFile file,
                                    HttpSession session){
        if (studentId == null) {
            Object sid = session == null ? null : session.getAttribute("userId");
            if (sid != null) {
                try { studentId = Long.valueOf(String.valueOf(sid)); } catch (Exception ignored) {}
            }
        }
        if (studentId == null) return ResponseEntity.status(401).body("请先登录或传入 studentId");
        String filePath = null;
        String fileType = null;
        if (file != null && !file.isEmpty()) {
            try {
                Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "submissions");
                Files.createDirectories(uploadDir);
                String safeName = System.currentTimeMillis() + "_" + (file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_"));
                Path target = uploadDir.resolve(safeName);
                file.transferTo(target.toFile());
                filePath = "uploads/submissions/" + safeName;
                fileType = file.getContentType();
            } catch (Exception e) {
                return ResponseEntity.status(500).body("保存文件失败");
            }
        }
        Submission s = new Submission(null, id, studentId, content, filePath, fileType, null, null, null, "SUBMITTED", null, LocalDateTime.now());
        Submission saved = assignments.submit(s);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/submissions/{id}/grade")
    public ResponseEntity<?> grade(@PathVariable Long id,
                                   @RequestParam Double grade,
                                   @RequestParam Long graderId,
                                   @RequestParam(required = false) String remarks){
        Submission s = assignments.gradeSubmission(id, grade, graderId, remarks);
        if (s == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(s);
    }

    @GetMapping("/{id}/submissions")
    public List<Submission> submissions(@PathVariable Long id){
        return assignments.listSubmissions(id);
    }

    @GetMapping("/submissions/{id}/file")
    public ResponseEntity<?> file(@PathVariable Long id){
        Optional<Submission> s = assignments.findSubmission(id);
        if (s.isEmpty()) return ResponseEntity.notFound().build();
        Submission sub = s.get();
        if (sub.filePath() != null) {
            try {
                Path p = Paths.get(System.getProperty("user.dir")).resolve(sub.filePath());
                if (!Files.exists(p)) return ResponseEntity.notFound().build();
                byte[] data = Files.readAllBytes(p);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, sub.fileType() != null ? sub.fileType() : "application/octet-stream")
                        .body(data);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("无法读取文件");
            }
        }
        return ResponseEntity.ok(Collections.emptyMap());
    }
}
