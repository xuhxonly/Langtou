package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.RecommendPosition;
import com.langtou.content.mapper.RecommendPositionMapper;
import com.langtou.content.service.RecommendPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendPositionServiceImpl implements RecommendPositionService {

    private final RecommendPositionMapper recommendPositionMapper;

    @Override
    public RecommendPosition createPosition(RecommendPosition position) {
        if (!StringUtils.hasText(position.getPositionType())) {
            throw new BusinessException("推荐位类型不能为空");
        }
        if (position.getStatus() == null) {
            position.setStatus("ACTIVE");
        }
        if (position.getSortOrder() == null) {
            position.setSortOrder(0);
        }

        recommendPositionMapper.insert(position);
        log.info("创建推荐位成功: id={}, type={}", position.getId(), position.getPositionType());
        return position;
    }

    @Override
    public RecommendPosition updatePosition(Long positionId, RecommendPosition position) {
        RecommendPosition existing = recommendPositionMapper.selectById(positionId);
        if (existing == null) {
            throw new BusinessException("推荐位不存在: " + positionId);
        }

        position.setId(positionId);
        recommendPositionMapper.updateById(position);
        log.info("更新推荐位成功: id={}", positionId);
        return recommendPositionMapper.selectById(positionId);
    }

    @Override
    public void deletePosition(Long positionId) {
        RecommendPosition existing = recommendPositionMapper.selectById(positionId);
        if (existing == null) {
            throw new BusinessException("推荐位不存在: " + positionId);
        }
        recommendPositionMapper.deleteById(positionId);
        log.info("删除推荐位成功: id={}", positionId);
    }

    @Override
    public List<RecommendPosition> getActivePositionsByType(String positionType) {
        QueryWrapper<RecommendPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("position_type", positionType);
        wrapper.eq("status", "ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w ->
                w.isNull("start_time").or().le("start_time", now)
        );
        wrapper.and(w ->
                w.isNull("end_time").or().ge("end_time", now)
        );

        wrapper.orderByDesc("sort_order");

        return recommendPositionMapper.selectList(wrapper);
    }

    @Override
    public PageResult<RecommendPosition> listPositions(Integer page, Integer size, String positionType, String status) {
        Page<RecommendPosition> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        QueryWrapper<RecommendPosition> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(positionType)) {
            wrapper.eq("position_type", positionType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("sort_order");

        Page<RecommendPosition> resultPage = recommendPositionMapper.selectPage(pageParam, wrapper);
        return PageResult.of(resultPage);
    }
}
