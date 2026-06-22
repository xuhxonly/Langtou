package com.langtou.ad.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.ad.entity.Advertisement;
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
