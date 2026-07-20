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
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料辅助服务实现。
 *
 * <p>默认头像先使用约定编码和静态路径，后续可替换为文件服务或对象存储地址。</p>
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    /** 上传头像最大体积：裁剪后的 PNG 在本地磁盘和页面加载之间保持合理开销。 */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    /** 自定义头像公开访问地址前缀，仅由服务端生成，不接受客户端传入。 */
    private static final String CUSTOM_AVATAR_URL_PREFIX = "/uploads/avatars/";

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
    private final Path avatarRoot;

    public UserProfileServiceImpl(SysUserMapper sysUserMapper, UserContextService userContextService,
                                  AuditLogService auditLogService,
                                  @Value("${opsdesk.storage.avatar-root:storage/avatars}") String avatarRoot) {
        this.sysUserMapper = sysUserMapper;
        this.userContextService = userContextService;
        this.auditLogService = auditLogService;
        this.avatarRoot = Path.of(avatarRoot).toAbsolutePath().normalize();
    }

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
        boolean keepCustomAvatar = "custom".equals(avatarCode)
                && user.getAvatarUrl() != null
                && user.getAvatarUrl().startsWith(CUSTOM_AVATAR_URL_PREFIX);
        AvatarOptionVO avatar = keepCustomAvatar ? null : listAvatarOptions(gender).options().stream()
                .filter(item -> item.avatarCode().equals(avatarCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "头像选项不合法"));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : user.getNickname());
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        String previousAvatarUrl = user.getAvatarUrl();
        user.setGender(gender);
        if (!keepCustomAvatar) {
            user.setAvatarCode(avatar.avatarCode());
            user.setAvatarUrl(avatar.avatarUrl());
        }
        user.setUpdateBy(userId);
        if (sysUserMapper.updateProfile(user) != 1) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "个人资料保存失败");
        if (!keepCustomAvatar) {
            deleteCustomAvatar(avatarRoot, previousAvatarUrl);
        }
        auditLogService.record(userId, "PROFILE_UPDATE", "USER", userId, "用户更新个人资料", requestIp, userAgent);
        return userContextService.loadUserVO(userId);
    }

    @Override
    public UserVO uploadAvatar(Long userId, MultipartFile file, String requestIp, String userAgent) {
        validateAvatarFile(file);
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在");
        }
        String fileName = userId + "-" + LocalDateTime.now().toString().replaceAll("[^0-9]", "") + ".png";
        Path root = avatarRoot;
        Path target = root.resolve(fileName);
        try {
            Files.createDirectories(root);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "头像上传失败");
        }

        String previousAvatarUrl = user.getAvatarUrl();
        user.setAvatarCode("custom");
        user.setAvatarUrl(CUSTOM_AVATAR_URL_PREFIX + fileName);
        user.setUpdateBy(userId);
        if (sysUserMapper.updateProfile(user) != 1) {
            deleteAvatarFile(target);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像保存失败");
        }
        deleteCustomAvatar(root, previousAvatarUrl);
        auditLogService.record(userId, "AVATAR_UPLOAD", "USER", userId, "用户更新自定义头像", requestIp, userAgent);
        return userContextService.loadUserVO(userId);
    }

    /** 前端裁剪输出 PNG；服务端再校验 MIME、体积与图片解码，防止伪装文件落盘。 */
    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE || !"image/png".equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像仅支持 2MB 以内的 PNG 图片");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "头像图片无法识别");
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像图片无法识别");
        }
    }

    /** 删除已替换的自定义头像；删除失败不影响资料保存，避免历史文件阻断主流程。 */
    private void deleteCustomAvatar(Path root, String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(CUSTOM_AVATAR_URL_PREFIX)) {
            return;
        }
        Path candidate = root.resolve(avatarUrl.substring(CUSTOM_AVATAR_URL_PREFIX.length())).normalize();
        if (candidate.startsWith(root)) {
            deleteAvatarFile(candidate);
        }
    }

    private void deleteAvatarFile(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 旧头像清理失败仅产生孤儿文件，不应影响本次用户资料操作。
        }
    }
}
