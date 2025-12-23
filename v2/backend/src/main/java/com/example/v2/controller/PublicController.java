package com.example.v2.controller;

import com.example.v2.model.Course;
import com.example.v2.model.Video;
import com.example.v2.service.CourseService;
import com.example.v2.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class PublicController {
    private final CourseService courses;
    private final VideoService videos;

    public PublicController(CourseService courses, VideoService videos) {
        this.courses = courses;
        this.videos = videos;
    }

    @GetMapping("/courses")
    public List<Course> allCourses(){
        return courses.listApproved();
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<Course> course(@PathVariable Integer id){
        return courses.find(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/courses/{id}/videos")
    public List<Video> courseVideos(@PathVariable Integer id){
        return videos.listByCourse(id);
    }
}
