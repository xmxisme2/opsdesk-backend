package com.opsdesk.user.service;

import com.opsdesk.user.vo.AvatarOptionsVO;
import com.opsdesk.user.dto.UserProfileUpdateRequest;
import com.opsdesk.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料辅助服务。
 *
 * <p>首版先提供默认头像选项，个人资料编辑在后续用户管理模块继续补齐。</p>
 */
public interface UserProfileService {

    AvatarOptionsVO listAvatarOptions(String gender);

    UserVO updateMyProfile(Long userId, UserProfileUpdateRequest request, String requestIp, String userAgent);
    UserVO uploadAvatar(Long userId, MultipartFile file, String requestIp, String userAgent);
}
