package com.opsdesk.user.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.user.dto.AvatarOptionsRequest;
import com.opsdesk.user.dto.UserProfileUpdateRequest;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.opsdesk.user.service.UserProfileService;
import com.opsdesk.user.vo.AvatarOptionsVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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

    /** 当前用户仅维护个人展示资料，账号和组织字段由后台用户管理统一控制。 */
    @PostMapping("/me/profile")
    @Idempotent
    public ApiResponse<UserVO> updateMyProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                                @Valid @RequestBody UserProfileUpdateRequest request,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.success(userProfileService.updateMyProfile(currentUser.getUserId(), request,
                servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Idempotent
    public ApiResponse<UserVO> uploadAvatar(@AuthenticationPrincipal CurrentUser currentUser, @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.success(userProfileService.uploadAvatar(currentUser.getUserId(), file, request.getRemoteAddr(), request.getHeader("User-Agent")));
    }
}
