package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.AdClick;
import com.langtou.content.entity.AdImpression;
import com.langtou.content.entity.Advertisement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdvertisementMapper extends BaseMapper<Advertisement> {

    @Update("UPDATE advertisement SET impressions = impressions + 1 WHERE id = #{adId}")
    int incrementImpressions(@Param("adId") Long adId);

    @Update("UPDATE advertisement SET clicks = clicks + 1 WHERE id = #{adId}")
    int incrementClicks(@Param("adId") Long adId);
}
