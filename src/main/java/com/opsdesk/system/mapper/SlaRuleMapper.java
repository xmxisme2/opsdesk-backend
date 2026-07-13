package com.opsdesk.system.mapper;

import com.opsdesk.system.entity.SlaRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SLA 规则数据访问 Mapper，业务 SQL 统一维护在 XML。 */
@Mapper
public interface SlaRuleMapper {
    List<SlaRule> search(@Param("categoryId") Long categoryId,
                         @Param("priority") String priority,
                         @Param("enabled") Integer enabled);
    SlaRule findById(@Param("id") Long id);
    SlaRule findByCategoryAndPriority(@Param("categoryId") Long categoryId,
                                      @Param("priority") String priority,
                                      @Param("excludeId") Long excludeId);
    int insert(SlaRule rule);
    int update(SlaRule rule);
    int disable(@Param("id") Long id, @Param("operatorId") Long operatorId);
}
