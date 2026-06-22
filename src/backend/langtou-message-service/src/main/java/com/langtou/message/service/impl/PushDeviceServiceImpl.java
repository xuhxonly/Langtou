package com.langtou.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.message.entity.PushDeviceToken;
import com.langtou.message.mapper.PushDeviceTokenMapper;
import com.langtou.message.service.PushDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推送设备管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushDeviceServiceImpl implements PushDeviceService {

    private final PushDeviceTokenMapper pushDeviceTokenMapper;

    @Override
    public PushDeviceToken registerDevice(Long userId, String deviceType, String deviceToken,
                                           String appVersion, String osVersion) {
        // 校验设备类型
        if (!"ANDROID".equalsIgnoreCase(deviceType) && !"IOS".equalsIgnoreCase(deviceType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的设备类型，仅支持ANDROID和IOS");
        }

        // 查询是否已存在该用户+Token的记录
        LambdaQueryWrapper<PushDeviceToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushDeviceToken::getUserId, userId)
               .eq(PushDeviceToken::getDeviceToken, deviceToken);
        PushDeviceToken existing = pushDeviceTokenMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在则更新信息
            existing.setDeviceType(deviceType.toUpperCase());
            existing.setAppVersion(appVersion);
            existing.setOsVersion(osVersion);
            existing.setLastActiveAt(LocalDateTime.now());
            pushDeviceTokenMapper.updateById(existing);
            log.info("更新设备Token: userId={}, deviceType={}, token={}", userId, deviceType, maskToken(deviceToken));
            return existing;
        }

        // 新增记录
        PushDeviceToken newToken = new PushDeviceToken();
        newToken.setUserId(userId);
        newToken.setDeviceType(deviceType.toUpperCase());
        newToken.setDeviceToken(deviceToken);
        newToken.setAppVersion(appVersion);
        newToken.setOsVersion(osVersion);
        newToken.setLastActiveAt(LocalDateTime.now());
        pushDeviceTokenMapper.insert(newToken);
        log.info("注册设备Token: userId={}, deviceType={}, token={}", userId, deviceType, maskToken(deviceToken));
        return newToken;
    }

    @Override
    public void unregisterDevice(Long userId, String deviceToken) {
        LambdaQueryWrapper<PushDeviceToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushDeviceToken::getUserId, userId)
               .eq(PushDeviceToken::getDeviceToken, deviceToken);
        int deleted = pushDeviceTokenMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备Token不存在");
        }
        log.info("注销设备Token: userId={}, token={}", userId, maskToken(deviceToken));
    }

    @Override
    public List<PushDeviceToken> getUserDevices(Long userId) {
        return pushDeviceTokenMapper.selectByUserId(userId);
    }

    /**
     * 对Token进行脱敏，仅显示前8位和后4位
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "****";
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
}
