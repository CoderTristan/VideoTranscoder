package com.example.fullsite;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;
    private final AwsCredentialsProvider credentialsProvider;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadVideo(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Template.upload(bucketName, fileName, inputStream, metadata);
        }

        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
    }
    public String uploadLocalFile(Path localFilePath, String originalName) throws IOException {
        String s3Key = UUID.randomUUID() + "_transcoded_" + originalName;

        try (InputStream inputStream = Files.newInputStream(localFilePath)) {
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType("video/mp4")
                    .contentLength(Files.size(localFilePath))
                    .build();

            s3Template.upload(bucketName, s3Key, inputStream, metadata);
        }

        return s3Key; 
    }
    public String generatePresignedUrl(String s3Key) {
        try (S3Presigner presigner = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_2)
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}
