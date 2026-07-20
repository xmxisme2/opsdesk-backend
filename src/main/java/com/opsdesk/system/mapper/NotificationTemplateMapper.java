package com.opsdesk.system.mapper;

import com.opsdesk.system.entity.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 通知模板数据访问 Mapper，业务 SQL 统一维护在 XML。 */
@Mapper
public interface NotificationTemplateMapper {
    List<NotificationTemplate> search(@Param("type") String type, @Param("channel") String channel);
    NotificationTemplate findById(@Param("id") Long id);
    NotificationTemplate findByTypeAndChannel(@Param("type") String type, @Param("channel") String channel);
    int update(NotificationTemplate template);
}
