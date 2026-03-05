package com.qrordering.storage.controller;

import com.qrordering.storage.service.MinioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Image upload API. Stores files in MinIO and returns public URL.
 */
@RestController
@RequestMapping("/upload")
@ConditionalOnBean(MinioService.class)
public class UploadController {

    private final MinioService minioService;

    public UploadController(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * Upload an image. Optional prefix for organization (e.g. logos, dishes).
     *
     * @param file   image file (JPEG, PNG, GIF, WebP; max 5MB)
     * @param prefix optional path prefix
     * @return { "url": "http://..." }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prefix", required = false) String prefix) {
        String url = minioService.uploadImage(file, prefix);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
