package com.opsdesk.knowledge.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 知识库分类树视图。 */
@Getter
@Setter
public class KnowledgeCategoryVO {
    private String id;
    private String parentId;
    private String name;
    private int sort;
    private boolean enabled;
    private List<KnowledgeCategoryVO> children = new ArrayList<>();
}
