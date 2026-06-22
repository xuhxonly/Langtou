package com.langtou.content.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String uploadFile(MultipartFile file, String objectName);

    String uploadFile(MultipartFile file);

    String getFileUrl(String objectName);
}
