package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.dto.ContentSearchDTO;
import com.langtou.content.dto.NoteDetailVO;
import com.langtou.content.dto.NoteFeedVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContentService {

    ContentDTO publish(ContentDTO contentDTO, Long userId);

    ContentDTO getContentById(Long id);

    List<ContentDTO> getUserContents(Long userId);

    void deleteContent(Long id, Long userId);

    List<ContentDTO> getFeed(int page, int size);

    /**
     * Feed流（分页，返回Feed卡片）
     */
    PageResult<NoteFeedVO> getFeedPage(int page, int size);

    /**
     * 个性化推荐Feed流（分页，返回Feed卡片）
     * 优先调用推荐服务，若推荐服务不可用则 fallback 到时间倒序
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 推荐笔记分页结果
     */
    PageResult<NoteFeedVO> getRecommendedFeed(Long userId, int page, int size);

    /**
     * 获取笔记详情（包含作者信息、互动数据）
     */
    NoteDetailVO getNoteDetail(Long noteId);

    /**
     * 编辑笔记
     */
    ContentDTO updateContent(Long noteId, Long userId, ContentDTO contentDTO);

    /**
     * 获取用户的笔记列表（分页）
     */
    PageResult<NoteFeedVO> getUserNotes(Long userId, int page, int size);

    /**
     * 笔记搜索
     */
    PageResult<NoteFeedVO> searchNotes(ContentSearchDTO searchDTO);

    /**
     * 图片/视频上传接口（返回URL）
     */
    String uploadFile(MultipartFile file);

    /**
     * 视频上传接口（限制视频格式和大小）
     */
    String uploadVideo(MultipartFile file);

    /**
     * 关注用户的Feed流（分页）
     */
    PageResult<NoteFeedVO> getFollowingFeed(Long userId, int page, int size);

    void incrementLikeCount(Long noteId);

    void decrementLikeCount(Long noteId);

    void incrementCommentCount(Long noteId);

    void incrementCollectCount(Long noteId);

    void decrementCollectCount(Long noteId);

    /**
     * 获取相关推荐笔记（基于相同标签）
     */
    PageResult<NoteFeedVO> getRelatedNotes(Long noteId, int page, int size);

    /**
     * 更新笔记可见性
     */
    void updateNoteVisibility(Long noteId, Long userId, Integer visibility);

    /**
     * 置顶/取消置顶笔记
     */
    void pinNote(Long noteId, Long userId, boolean pin);
}
