package com.langtou.ad.service;

import com.langtou.common.result.PageResult;
import com.langtou.ad.entity.RecommendPosition;

import java.util.List;

public interface RecommendPositionService {

    RecommendPosition createPosition(RecommendPosition position);

    RecommendPosition updatePosition(Long positionId, RecommendPosition position);

    void deletePosition(Long positionId);

    List<RecommendPosition> getActivePositionsByType(String positionType);

    PageResult<RecommendPosition> listPositions(Integer page, Integer size, String positionType, String status);
}
