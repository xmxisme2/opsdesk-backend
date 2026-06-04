package com.opsdesk.user.service.impl;

import com.opsdesk.user.service.UserProfileService;
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

    private static final List<AvatarOptionVO> MALE_OPTIONS = List.of(
            new AvatarOptionVO("avatar_male_01", "/assets/avatars/avatar_male_01.png", "男生默认头像 1"),
            new AvatarOptionVO("avatar_male_02", "/assets/avatars/avatar_male_02.png", "男生默认头像 2")
    );

    private static final List<AvatarOptionVO> FEMALE_OPTIONS = List.of(
            new AvatarOptionVO("avatar_female_01", "/assets/avatars/avatar_female_01.png", "女生默认头像 1"),
            new AvatarOptionVO("avatar_female_02", "/assets/avatars/avatar_female_02.png", "女生默认头像 2")
    );

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
}
