package com.example.v2.controller;

import com.example.v2.model.Video;
import com.example.v2.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/videos")
public class VideoController {
    private final VideoService videos;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public VideoController(VideoService videos) {
        this.videos = videos;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam Integer teacherId,
                                    @RequestParam Integer courseId,
                                    @RequestParam MultipartFile file,
                                    HttpSession session) throws IOException {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("No file");

        Object sid = session == null ? null : session.getAttribute("userId");
        Object role = session == null ? null : session.getAttribute("role");
        Integer sessionUserId = null;
        try { if (sid != null) sessionUserId = Integer.valueOf(String.valueOf(sid)); } catch (Exception ignored) {}
        boolean isAdmin = "ADMIN".equals(role);
        boolean isTeacherOwner = "TEACHER".equals(role) && sessionUserId != null && sessionUserId.equals(teacherId);
        boolean allowed = isAdmin || isTeacherOwner;
        if (!allowed) return ResponseEntity.status(403).body("forbidden");

        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "video" : file.getOriginalFilename());
        Path teacherDir = Path.of(uploadDir, "teacher_" + teacherId);
        Path courseDir = teacherDir.resolve("course_" + courseId);
        Files.createDirectories(courseDir);
        Path target = courseDir.resolve(filename);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String thumbnailPath = null;
        try {
            var which = new ProcessBuilder("which", "ffmpeg").redirectErrorStream(true).start();
            int wexit = which.waitFor();
            if (wexit == 0) {
                String baseName = filename;
                int idx = baseName.lastIndexOf('.');
                if (idx > 0) baseName = baseName.substring(0, idx);
                Path thumb = courseDir.resolve(baseName + "_thumb.jpg");
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", target.toString(), "-ss", "00:00:01", "-vframes", "1", thumb.toString());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                int exit = p.waitFor();
                if (exit == 0 && Files.exists(thumb)) thumbnailPath = thumb.toString();
            }
        } catch (Exception ignored) {}

        String title = filename;
        String contentType = file.getContentType();
        long size = file.getSize();
        videos.save(courseId, teacherId, filename, target.toString(), thumbnailPath, title, contentType, size);
        Map<String,Object> res = new HashMap<>();
        res.put("filename", filename);
        res.put("path", target.toString());
        if (thumbnailPath != null) res.put("thumbnailPath", thumbnailPath);
        res.put("size", size);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/teachers/{teacherId}/courses")
    public List<Video> teacherVideos(@PathVariable Integer teacherId){
        return videos.listByTeacher(teacherId);
    }

    @GetMapping("/teachers/{teacherId}")
    public List<Video> teacherVideosPublic(@PathVariable Integer teacherId){
        // exposed for public gallery use
        return videos.listPublicByTeacher(teacherId);
    }

    @GetMapping("/courses/{courseId}/videos")
    public List<Video> courseVideos(@PathVariable Integer courseId){
        return videos.listByCourse(courseId);
    }

    @GetMapping("/thumb/{id}")
    public ResponseEntity<Resource> thumbnail(@PathVariable Long id) throws IOException {
        Video v = videos.findById(id);
        if (v == null || v.thumbnailPath() == null) return ResponseEntity.notFound().build();
        Path file = Path.of(v.thumbnailPath());
        if (!Files.exists(file)) return ResponseEntity.notFound().build();
        FileSystemResource res = new FileSystemResource(file);
        String mime = "image/jpeg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(res.contentLength()))
                .body(res);
    }

    @GetMapping("/serve/{teacherId}/{courseId}/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String teacherId,
                                          @PathVariable String courseId,
                                          @PathVariable String filename) throws IOException {
        Path file = Path.of(uploadDir, "teacher_" + teacherId, "course_" + courseId, filename);
        if (!Files.exists(file)) return ResponseEntity.notFound().build();
        FileSystemResource res = new FileSystemResource(file);
        String mime = URLConnection.guessContentTypeFromName(filename);
        if (mime == null) mime = "application/octet-stream";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(res.contentLength()))
                .body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) throws IOException {
        Video v = videos.findById(id);
        if (v == null) return ResponseEntity.notFound().build();
        Object role = session == null ? null : session.getAttribute("role");
        Object sid = session == null ? null : session.getAttribute("userId");
        Integer uid = null;
        try { if (sid != null) uid = Integer.valueOf(String.valueOf(sid)); } catch (Exception ignored) {}
        boolean isAdmin = "ADMIN".equals(role);
        boolean isOwnerTeacher = "TEACHER".equals(role) && uid != null && v.teacherId() != null && uid.equals(v.teacherId());
        if (!isAdmin && !isOwnerTeacher) return ResponseEntity.status(403).body("forbidden");

        deleteFiles(v);
        videos.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<?> bulkDelete(@RequestBody List<Long> ids, HttpSession session) throws IOException {
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body("forbidden");
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().body("empty ids");
        List<Video> toDelete = new ArrayList<>();
        for (Long id : ids) { if (id == null) continue; Video v = videos.findById(id); if (v != null) toDelete.add(v); }
        for (Video v : toDelete) { deleteFiles(v); }
        videos.deleteBulk(ids);
        return ResponseEntity.ok(Map.of("deleted", toDelete.size()));
    }

    @GetMapping("/review")
    public ResponseEntity<?> reviewList(HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body("forbidden");
        return ResponseEntity.ok(videos.listPending());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") int approved,
                                     @RequestParam(required = false) String remark,
                                     HttpSession session){
        Object role = session == null ? null : session.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body("forbidden");
        Video v = videos.findById(id);
        if (v == null) return ResponseEntity.notFound().build();
        int normalized = approved>0?1:-1; // 1=通过, -1=不通过, 0=待审
        videos.setApproved(id, normalized, remark);
        return ResponseEntity.ok(Map.of("ok", true, "approved", normalized));
    }

    private void deleteFiles(Video v) {
        try { if (v.path() != null) Files.deleteIfExists(Path.of(v.path())); } catch (Exception ignored) {}
        try { if (v.thumbnailPath() != null) Files.deleteIfExists(Path.of(v.thumbnailPath())); } catch (Exception ignored) {}
    }
}
