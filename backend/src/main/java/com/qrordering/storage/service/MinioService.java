package com.qrordering.storage.service;

import com.qrordering.storage.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Upload files to MinIO and return public URLs.
 * Bucket is created if missing and set to public read for stored objects.
 */
@Service
@ConditionalOnBean(MinioClient.class)
public class MinioService {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ALLOWED_TYPES = "image/jpeg,image/png,image/gif,image/webp";

    private final MinioClient minioClient;
    private final MinioProperties props;

    private boolean bucketEnsured;

    public MinioService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    private void ensureBucket() {
        if (bucketEnsured) return;
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {"AWS": ["*"]},
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(props.getBucket());
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(props.getBucket()).config(policy).build());
            }
            bucketEnsured = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket: " + e.getMessage(), e);
        }
    }

    /**
     * Upload an image file. Validates type and size.
     *
     * @param file   multipart file (image)
     * @param prefix optional path prefix (e.g. "logos", "dishes")
     * @return public URL of the stored object
     */
    public String uploadImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Allowed types: " + ALLOWED_TYPES);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Max file size 5MB");
        }
        ensureBucket();
        String ext = contentType.replace("image/", "");
        String objectName = (prefix != null && !prefix.isBlank() ? prefix + "/" : "")
                + UUID.randomUUID() + "." + ext;
        try {
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(props.getBucket())
                        .object(objectName)
                        .stream(is, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
            String base = props.getPublicUrl().replaceAll("/$", "");
            return base + "/" + props.getBucket() + "/" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
}
