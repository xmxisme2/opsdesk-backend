package com.opsdesk.user.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.user.dto.AvatarOptionsRequest;
import com.opsdesk.user.service.UserProfileService;
import com.opsdesk.user.vo.AvatarOptionsVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料辅助接口 Controller。
 *
 * <p>首版提供默认头像选项，用户管理和个人资料编辑后续按模块继续补齐。</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/avatar-options")
    public ApiResponse<AvatarOptionsVO> avatarOptions(@RequestBody(required = false) AvatarOptionsRequest request) {
        String gender = request == null ? null : request.getGender();
        return ApiResponse.success(userProfileService.listAvatarOptions(gender));
    }
}
