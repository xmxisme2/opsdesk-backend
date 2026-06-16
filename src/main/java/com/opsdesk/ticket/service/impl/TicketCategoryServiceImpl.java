package com.opsdesk.ticket.service.impl;

import com.opsdesk.team.entity.Team;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.service.TicketCategoryService;
import com.opsdesk.ticket.vo.TicketCategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单分类服务实现。
 *
 * <p>按 parent_id 组装树结构，并补充默认处理团队名称，供创建工单页面直接渲染。</p>
 */
@Service
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryMapper ticketCategoryMapper;
    private final TeamMapper teamMapper;
    private final TicketConverter ticketConverter;

    public TicketCategoryServiceImpl(TicketCategoryMapper ticketCategoryMapper,
                                     TeamMapper teamMapper,
                                     TicketConverter ticketConverter) {
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.teamMapper = teamMapper;
        this.ticketConverter = ticketConverter;
    }

    @Override
    public List<TicketCategoryVO> tree(TicketCategoryTreeRequest request) {
        TicketCategoryTreeRequest safeRequest = request == null ? new TicketCategoryTreeRequest() : request;
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);
        List<TicketCategory> categories = ticketCategoryMapper.searchTree(enabled, safeRequest.normalizedKeyword());
        Set<Long> visibleIds = categories.stream().map(TicketCategory::getId).collect(Collectors.toSet());
        Map<Long, List<TicketCategory>> childrenByParentId = categories.stream()
                .collect(Collectors.groupingBy(category -> category.getParentId() == null ? 0L : category.getParentId()));
        return categories.stream()
                .filter(category -> category.getParentId() == null || !visibleIds.contains(category.getParentId()))
                .map(category -> toTreeVO(category, childrenByParentId))
                .toList();
    }

    private TicketCategoryVO toTreeVO(TicketCategory category, Map<Long, List<TicketCategory>> childrenByParentId) {
        List<TicketCategoryVO> children = childrenByParentId.getOrDefault(category.getId(), List.of())
                .stream()
                .map(child -> toTreeVO(child, childrenByParentId))
                .toList();
        Team defaultTeam = category.getDefaultTeamId() == null ? null : teamMapper.findById(category.getDefaultTeamId());
        return ticketConverter.toCategoryVO(category, defaultTeam, children);
    }
}
