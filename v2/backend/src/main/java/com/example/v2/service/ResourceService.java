package com.example.v2.service;

import com.example.v2.model.Resource;
import com.example.v2.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {
    private final ResourceRepository repo;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ResourceService(ResourceRepository repo) { this.repo = repo; }

    public List<Resource> list(){ return repo.findAll(); }
    public Optional<Resource> find(Long id){ return repo.findById(id); }

    public Resource saveToDb(Resource r){ return repo.save(r); }
    public void delete(Long id){ repo.delete(id); }

    public Resource upload(MultipartFile file, String name){
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("文件为空");
        String filename = file.getOriginalFilename();
        String ct = file.getContentType();
        if (ct == null) ct = "application/octet-stream";
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String safeName = System.currentTimeMillis() + "_" + (filename == null ? "upload" : filename.replaceAll("[^a-zA-Z0-9._-]","_"));
            Path dest = uploadPath.resolve(safeName);
            file.transferTo(dest.toFile());
            String url = "/api/v2/resources/serve/" + safeName;
            Resource r = new Resource(null, name != null && !name.isBlank() ? name : safeName, url, null, ct, LocalDateTime.now(), dest.toString());
            return repo.save(r);
        } catch (Exception e) {
            throw new RuntimeException("保存文件失败", e);
        }
    }
}
