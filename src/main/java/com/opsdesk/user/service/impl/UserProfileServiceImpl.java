package com.opsdesk.user.service.impl;

import com.opsdesk.user.service.UserProfileService;
import com.opsdesk.user.dto.UserProfileUpdateRequest;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.service.UserContextService;
import com.opsdesk.user.vo.UserVO;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.user.vo.AvatarOptionVO;
import com.opsdesk.user.vo.AvatarOptionsVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户资料辅助服务实现。
 *
 * <p>默认头像先使用约定编码和静态路径，后续可替换为文件服务或对象存储地址。</p>
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    /** 男性默认头像选项：注册和个人资料页在未上传头像时使用，不允许前端随意传入不存在的编码。 */
    private static final List<AvatarOptionVO> MALE_OPTIONS = List.of(
            new AvatarOptionVO("avatar_male_01", "/assets/avatars/avatar_male_01.png", "男生默认头像 1"),
            new AvatarOptionVO("avatar_male_02", "/assets/avatars/avatar_male_02.png", "男生默认头像 2")
    );

    /** 女性默认头像选项：注册和个人资料页在未上传头像时使用，不允许前端随意传入不存在的编码。 */
    private static final List<AvatarOptionVO> FEMALE_OPTIONS = List.of(
            new AvatarOptionVO("avatar_female_01", "/assets/avatars/avatar_female_01.png", "女生默认头像 1"),
            new AvatarOptionVO("avatar_female_02", "/assets/avatars/avatar_female_02.png", "女生默认头像 2")
    );
    private final SysUserMapper sysUserMapper;
    private final UserContextService userContextService;
    private final AuditLogService auditLogService;

    public UserProfileServiceImpl(SysUserMapper sysUserMapper, UserContextService userContextService,
                                  AuditLogService auditLogService) { this.sysUserMapper = sysUserMapper; this.userContextService = userContextService; this.auditLogService = auditLogService; }

    @Override
    public AvatarOptionsVO listAvatarOptions(String gender) {
        if (!StringUtils.hasText(gender)) {
            return new AvatarOptionsVO(null, List.of(
                    MALE_OPTIONS.get(0),
                    MALE_OPTIONS.get(1),
                    FEMALE_OPTIONS.get(0),
                    FEMALE_OPTIONS.get(1)
            ));
        }
        if ("FEMALE".equalsIgnoreCase(gender)) {
            return new AvatarOptionsVO("FEMALE", FEMALE_OPTIONS);
        }
        return new AvatarOptionsVO("MALE", MALE_OPTIONS);
    }

    @Override
    public UserVO updateMyProfile(Long userId, UserProfileUpdateRequest request, String requestIp, String userAgent) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在");
        String gender = "FEMALE".equalsIgnoreCase(request.getGender()) ? "FEMALE" : "MALE";
        String avatarCode = StringUtils.hasText(request.getAvatarCode()) ? request.getAvatarCode().trim() : user.getAvatarCode();
        AvatarOptionVO avatar = listAvatarOptions(gender).options().stream().filter(item -> item.avatarCode().equals(avatarCode)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "头像选项不合法"));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : user.getNickname());
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        user.setGender(gender); user.setAvatarCode(avatar.avatarCode()); user.setAvatarUrl(avatar.avatarUrl()); user.setUpdateBy(userId);
        if (sysUserMapper.updateProfile(user) != 1) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "个人资料保存失败");
        auditLogService.record(userId, "PROFILE_UPDATE", "USER", userId, "用户更新个人资料", requestIp, userAgent);
        return userContextService.loadUserVO(userId);
    }
}
