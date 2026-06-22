package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.RecommendPosition;

import java.util.List;

/**
 * 推荐位服务接口
 */
public interface RecommendPositionService {

    /**
     * 创建推荐位
     */
    RecommendPosition createPosition(RecommendPosition position);

    /**
     * 更新推荐位
     */
    RecommendPosition updatePosition(Long positionId, RecommendPosition position);

    /**
     * 删除推荐位
     */
    void deletePosition(Long positionId);

    /**
     * 按位置类型获取活跃推荐位（用户端）
     */
    List<RecommendPosition> getActivePositionsByType(String positionType);

    /**
     * 推荐位列表（管理端）
     */
    PageResult<RecommendPosition> listPositions(Integer page, Integer size, String positionType, String status);
}
