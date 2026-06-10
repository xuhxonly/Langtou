package com.langtou.content.service;

import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Content;

import java.util.List;

public interface ContentService {

    ContentDTO publish(ContentDTO contentDTO, Long userId);

    ContentDTO getContentById(Long id);

    List<ContentDTO> getUserContents(Long userId);

    void deleteContent(Long id, Long userId);

    List<ContentDTO> getFeed(int page, int size);
}
