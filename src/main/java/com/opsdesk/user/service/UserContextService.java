package com.opsdesk.user.service;

import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.user.vo.UserVO;

/**
 * 用户上下文服务。
 *
 * <p>统一加载当前用户资料、角色和权限，供登录返回和 JWT 过滤器复用。</p>
 */
public interface UserContextService {

    CurrentUser loadCurrentUser(Long userId);

    UserVO loadUserVO(Long userId);
}
