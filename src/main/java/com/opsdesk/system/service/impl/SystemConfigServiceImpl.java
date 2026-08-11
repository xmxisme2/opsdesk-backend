package com.opsdesk.system.service.impl;

import com.opsdesk.system.dto.SystemConfigSearchRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.SystemConfigService;
import com.opsdesk.system.vo.SystemConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/** 系统通用配置查询实现，集中执行条件规范化和敏感值脱敏。 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    /** 敏感配置统一展示值，调用方不能通过该接口获取任何真实凭据。 */
    private static final String MASKED_VALUE = "******";

    private final SystemConfigMapper mapper;

    public SystemConfigServiceImpl(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SystemConfigVO> search(SystemConfigSearchRequest request) {
        String group = request == null || !StringUtils.hasText(request.group())
                ? null : request.group().trim().toUpperCase(Locale.ROOT);
        String keyword = request == null || !StringUtils.hasText(request.keyword())
                ? null : request.keyword().trim();
        return mapper.search(group, keyword).stream().map(this::toVO).toList();
    }

    private SystemConfigVO toVO(SystemConfig config) {
        String value = Integer.valueOf(1).equals(config.getSensitive()) ? MASKED_VALUE : config.getConfigValue();
        return new SystemConfigVO(config.getConfigKey(), value, config.getConfigGroup(),
                config.getDescription(), Integer.valueOf(1).equals(config.getEditable()));
    }
}
