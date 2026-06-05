package com.opsdesk.user.service;

import com.opsdesk.user.dto.UserRoleUpdateRequest;
import com.opsdesk.user.vo.UserVO;

/**
 * 用户角色服务。
 *
 * <p>负责后台用户管理页的角色整体替换，并同步刷新用户权限上下文缓存。</p>
 */
public interface UserRoleService {

    UserVO updateUserRoles(String userId,
                           UserRoleUpdateRequest request,
                           Long operatorId,
                           String requestIp,
                           String userAgent);
}
