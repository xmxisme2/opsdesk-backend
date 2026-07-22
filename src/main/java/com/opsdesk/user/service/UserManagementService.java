package com.opsdesk.user.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.user.dto.UserCreateRequest;
import com.opsdesk.user.dto.UserResetPasswordRequest;
import com.opsdesk.user.dto.UserSearchRequest;
import com.opsdesk.user.dto.UserStatusUpdateRequest;
import com.opsdesk.user.dto.UserUpdateRequest;
import com.opsdesk.user.vo.UserResetPasswordVO;
import com.opsdesk.user.vo.UserVO;

/**
 * 后台用户管理服务。
 *
 * <p>集中处理用户列表、创建、详情、编辑、启停和重置密码等管理员操作，Controller 只负责权限入口和参数接收。</p>
 */
public interface UserManagementService {

    PageResult<UserVO> search(UserSearchRequest request);

    UserVO create(UserCreateRequest request, Long operatorId, String requestIp, String userAgent);

    UserVO detail(String id);

    UserVO update(String id, UserUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    UserVO updateStatus(String id, UserStatusUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    /** 管理员解除系统风控导致的账号锁定，并清理该账号的登录失败记录。 */
    UserVO unlock(String id, Long operatorId, String requestIp, String userAgent);

    void delete(String id, Long operatorId, String requestIp, String userAgent);

    UserResetPasswordVO resetPassword(String id,
                                      UserResetPasswordRequest request,
                                      Long operatorId,
                                      String requestIp,
                                      String userAgent);
}
