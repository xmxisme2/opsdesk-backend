package com.opsdesk.system.mapper;

import com.opsdesk.system.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系统配置数据访问 Mapper，查询和更新 SQL 统一维护在 XML。 */
@Mapper
public interface SystemConfigMapper {
    List<SystemConfig> findByGroup(@Param("group") String group);

    /** 按可选分组和关键字检索配置，敏感值由 Service 统一脱敏。 */
    List<SystemConfig> search(@Param("group") String group, @Param("keyword") String keyword);

    int updateValue(@Param("key") String key, @Param("value") String value, @Param("operatorId") Long operatorId);
}
