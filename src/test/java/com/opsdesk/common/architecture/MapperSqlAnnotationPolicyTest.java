package com.opsdesk.common.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper SQL 位置约束测试。
 *
 * <p>项目约定业务 SQL 统一维护在 MyBatis XML 中，Mapper 接口只保留方法签名，避免注解 SQL 与 XML SQL 分散维护。</p>
 */
class MapperSqlAnnotationPolicyTest {

    /** 禁止出现在业务 Mapper 接口中的 MyBatis SQL 注解。 */
    private static final Pattern SQL_ANNOTATION_PATTERN =
            Pattern.compile("@(?:Select|Insert|Update|Delete)(?:Provider)?\\b");

    @Test
    void mapperInterfacesShouldNotUseSqlAnnotations() throws IOException {
        Path mapperRoot = Path.of("src/main/java/com/opsdesk");
        List<Path> mapperFiles;
        try (var stream = Files.walk(mapperRoot)) {
            mapperFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Mapper.java"))
                    .toList();
        }

        List<String> violations = mapperFiles.stream()
                .filter(this::containsSqlAnnotation)
                .map(Path::toString)
                .toList();

        assertThat(violations)
                .as("业务 Mapper SQL 必须写在 src/main/resources/mapper/**/*.xml 中")
                .isEmpty();
    }

    private boolean containsSqlAnnotation(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return SQL_ANNOTATION_PATTERN.matcher(content).find();
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Mapper 文件失败: " + path, exception);
        }
    }
}
