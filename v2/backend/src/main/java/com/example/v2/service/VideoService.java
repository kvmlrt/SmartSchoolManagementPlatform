package com.example.v2.service;

import com.example.v2.model.Video;
import com.example.v2.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoService {
    private final VideoRepository repo;
    public VideoService(VideoRepository repo) { this.repo = repo; }
    public List<Video> listByTeacher(Integer teacherId){ return repo.findByTeacher(teacherId); }
    public List<Video> listPublicByTeacher(Integer teacherId){ return repo.findByTeacherPublic(teacherId); }
    public List<Video> listByCourse(Integer courseId){ return repo.findByCourse(courseId); }
    public Video findById(Long id){ return repo.findById(id); }
    public List<Video> listPending(){ return repo.findPending(); }

    public void save(Integer courseId,
                     Integer teacherId,
                     String filename,
                     String path,
                     String thumbnailPath,
                     String title,
                     String contentType,
                     Long size) {
        repo.save(courseId, teacherId, filename, path, thumbnailPath, title, contentType, size);
    }

    public void delete(Long id){ repo.deleteById(id); }
    public void deleteBulk(List<Long> ids){ repo.deleteByIds(ids); }
    public void setApproved(Long id, int approved, String remark){ repo.setApproved(id, approved, remark); }
}
