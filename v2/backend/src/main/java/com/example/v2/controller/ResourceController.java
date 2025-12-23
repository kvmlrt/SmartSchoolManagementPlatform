package com.example.v2.controller;

import com.example.v2.model.Resource;
import com.example.v2.service.ResourceService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/resources")
public class ResourceController {
    private final ResourceService resources;
    public ResourceController(ResourceService resources) { this.resources = resources; }

    @GetMapping
    public List<Resource> list(){ return resources.list(); }

    @PostMapping
    public Resource upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "name", required = false) String name){
        return resources.upload(file, name);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        resources.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/serve/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> serve(@PathVariable String filename){
        try {
            // prefer file-based
            Path p = Path.of(System.getProperty("user.dir"), "uploads", filename);
            if (Files.exists(p)) {
                FileSystemResource res = new FileSystemResource(p);
                String mime = Files.probeContentType(p);
                if (mime == null) mime = "application/octet-stream";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, mime)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(res.contentLength()))
                        .body(res);
            }
        } catch (Exception ignored) {}
        // fallback: find by url/file_path in DB
        List<Resource> all = resources.list();
        Optional<Resource> hit = all.stream().filter(r -> filename.equalsIgnoreCase(r.filePath()!=null? Path.of(r.filePath()).getFileName().toString() : null) || (r.url()!=null && r.url().endsWith(filename))).findFirst();
        if (hit.isPresent()) {
            Resource r = hit.get();
            if (r.filePath() != null) {
                Path p = Path.of(r.filePath());
                if (Files.exists(p)) {
                    FileSystemResource res = new FileSystemResource(p);
                    String mime = r.contentType()!=null? r.contentType() : "application/octet-stream";
                    try {
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, mime)
                                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(res.contentLength()))
                                .body(res);
                    } catch (Exception ignored) {}
                }
            }
            if (r.data() != null) {
                ByteArrayResource res = new ByteArrayResource(r.data());
                String mime = r.contentType()!=null? r.contentType() : "application/octet-stream";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, mime)
                        .body(res);
            }
        }
        return ResponseEntity.notFound().build();
    }
}
