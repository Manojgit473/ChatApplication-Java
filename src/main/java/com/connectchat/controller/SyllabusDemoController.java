package com.connectchat.controller;

import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.SyllabusDemoResultDto;
import com.connectchat.service.SyllabusDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller showcasing specific college syllabus concepts for viva demonstration.
 */
@RestController
@RequestMapping("/api/demo")
public class SyllabusDemoController {

    private final SyllabusDemoService syllabusDemoService;

    public SyllabusDemoController(SyllabusDemoService syllabusDemoService) {
        this.syllabusDemoService = syllabusDemoService;
    }

    @GetMapping("/hibernate-session")
    public ResponseEntity<ApiResponse<SyllabusDemoResultDto>> demonstrateHibernateSession() {
        SyllabusDemoResultDto result = syllabusDemoService.demonstrateHibernateSessionAndTransaction();
        return ResponseEntity.ok(ApiResponse.success("Hibernate Session & Transaction executed", result));
    }

    @GetMapping("/reflection")
    public ResponseEntity<ApiResponse<SyllabusDemoResultDto>> demonstrateReflection() {
        SyllabusDemoResultDto result = syllabusDemoService.demonstrateReflection();
        return ResponseEntity.ok(ApiResponse.success("Java Reflection demo executed", result));
    }

    @GetMapping("/serialization")
    public ResponseEntity<ApiResponse<SyllabusDemoResultDto>> demonstrateSerialization() {
        SyllabusDemoResultDto result = syllabusDemoService.demonstrateSerialization();
        return ResponseEntity.ok(ApiResponse.success("Java Serialization demo executed", result));
    }

    @GetMapping("/generics-streams")
    public ResponseEntity<ApiResponse<SyllabusDemoResultDto>> demonstrateGenericsStreams() {
        SyllabusDemoResultDto result = syllabusDemoService.demonstrateGenericsAndStreams();
        return ResponseEntity.ok(ApiResponse.success("Java Generics & Streams demo executed", result));
    }
}

