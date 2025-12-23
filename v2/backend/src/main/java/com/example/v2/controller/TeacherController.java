package com.example.v2.controller;

import com.example.v2.model.Course;
import com.example.v2.model.Video;
import com.example.v2.service.CourseService;
import com.example.v2.service.VideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/teachers")
public class TeacherController {
    private final CourseService courses;
    private final VideoService videos;
    public TeacherController(CourseService courses, VideoService videos) {
        this.courses = courses;
        this.videos = videos;
    }

    @GetMapping("/{teacherId}/courses")
    public List<Course> teacherCourses(@PathVariable Integer teacherId){
        return courses.listByTeacher(teacherId);
    }

    @GetMapping("/{teacherId}/videos")
    public List<Video> teacherVideos(@PathVariable Integer teacherId){
        return videos.listByTeacher(teacherId);
    }
}
