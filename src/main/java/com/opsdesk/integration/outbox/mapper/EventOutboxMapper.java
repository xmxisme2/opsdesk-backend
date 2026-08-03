package com.opsdesk.integration.outbox.mapper;

import com.opsdesk.integration.outbox.entity.EventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Outbox 数据访问 Mapper，全部 SQL 位于 XML。
 */
@Mapper
public interface EventOutboxMapper {

    int insert(EventOutbox event);

    List<EventOutbox> findDispatchable(@Param("limit") int limit);

    int claim(@Param("id") Long id);

    int markPublished(@Param("id") Long id);

    int markFailed(@Param("id") Long id,
                   @Param("lastError") String lastError,
                   @Param("retryDelaySeconds") long retryDelaySeconds);

    int recoverStaleSending();
}
