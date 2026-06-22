package com.langtou.content.service.impl;

import com.langtou.content.config.MinioConfig;
import com.langtou.content.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadFile(MultipartFile file, String objectName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return getFileUrl(objectName);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | InsufficientDataException |
                 InternalException | InvalidResponseException | NoSuchBucketException | XmlParserException |
                 ErrorResponseException | ServerException e) {
            log.error("MinIO文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString().replace("-", "") + extension;
        return uploadFile(file, objectName);
    }

    @Override
    public String getFileUrl(String objectName) {
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
    }
}
