package com.opsdesk.knowledge.mapper;

import com.opsdesk.knowledge.entity.KnowledgeTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 知识库标签数据访问 Mapper。 */
@Mapper
public interface KnowledgeTagMapper {
    List<KnowledgeTag> search(@Param("keyword") String keyword, @Param("limit") int limit);
    KnowledgeTag findById(@Param("id") Long id);
    KnowledgeTag findByName(@Param("name") String name);
    int insert(KnowledgeTag tag);
    int logicalDelete(@Param("id") Long id, @Param("operatorId") Long operatorId);
    int increaseCount(@Param("id") Long id);
    int decreaseCount(@Param("id") Long id);
}
