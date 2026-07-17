package com.opsdesk.knowledge.mapper;

import com.opsdesk.knowledge.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 知识库分类数据访问 Mapper。 */
@Mapper
public interface KnowledgeCategoryMapper {
    List<KnowledgeCategory> searchTree(@Param("enabled") Integer enabled);
    KnowledgeCategory findById(@Param("id") Long id);
    int countByParentAndName(@Param("parentId") Long parentId, @Param("name") String name,
                             @Param("excludeId") Long excludeId);
    int countChildren(@Param("id") Long id);
    int insert(KnowledgeCategory category);
    int update(KnowledgeCategory category);
    int logicalDelete(@Param("id") Long id, @Param("operatorId") Long operatorId);
}
