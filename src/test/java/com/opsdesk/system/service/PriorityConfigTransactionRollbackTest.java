package com.opsdesk.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.audit.entity.AuditLog;
import com.opsdesk.audit.mapper.AuditLogMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.audit.service.impl.AuditLogServiceImpl;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.system.dto.PriorityConfigUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.impl.PriorityConfigServiceImpl;
import com.opsdesk.system.vo.PriorityOptionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 优先级整体更新真实事务测试，锁定严格审计失败时四项配置必须同时回滚。 */
class PriorityConfigTransactionRollbackTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestTransactionConfig.class);
        jdbcTemplate = context.getBean(JdbcTemplate.class);
        jdbcTemplate.execute("""
                CREATE TABLE system_config (
                  config_key VARCHAR(64) PRIMARY KEY,
                  config_value VARCHAR(500) NOT NULL,
                  config_group VARCHAR(32) NOT NULL,
                  editable INT NOT NULL,
                  deleted INT NOT NULL,
                  update_by BIGINT,
                  update_time TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE audit_log (
                  id BIGINT PRIMARY KEY,
                  operator_id BIGINT,
                  operation_type VARCHAR(32) NOT NULL,
                  biz_type VARCHAR(32) NOT NULL CHECK (biz_type <> 'SYSTEM_CONFIG'),
                  biz_id BIGINT,
                  content VARCHAR(500),
                  request_ip VARCHAR(64),
                  user_agent VARCHAR(255),
                  create_by BIGINT,
                  update_by BIGINT
                )
                """);
        originalValues().forEach((key, value) -> jdbcTemplate.update(
                "INSERT INTO system_config(config_key, config_value, config_group, editable, deleted) VALUES (?, ?, 'PRIORITY', 1, 0)",
                key, value));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void updateShouldRollbackAllFourValuesWhenStrictAuditInsertFails() {
        PriorityConfigService service = context.getBean(PriorityConfigService.class);

        assertThatThrownBy(() -> service.update(changedRequest(), 9L, "127.0.0.1", "JUnit"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(readValues()).containsExactlyEntriesOf(originalValues());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class)).isZero();
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    private Map<String, String> readValues() {
        Map<String, String> values = new LinkedHashMap<>();
        jdbcTemplate.queryForList("SELECT config_key, config_value FROM system_config ORDER BY config_key")
                .forEach(row -> values.put((String) row.get("CONFIG_KEY"), (String) row.get("CONFIG_VALUE")));
        return values;
    }

    private Map<String, String> originalValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("priority.HIGH", "{\"name\":\"高\",\"sort\":30,\"color\":\"#BA630F\",\"enabled\":true}");
        values.put("priority.LOW", "{\"name\":\"低\",\"sort\":10,\"color\":\"#0D8052\",\"enabled\":true}");
        values.put("priority.MEDIUM", "{\"name\":\"中\",\"sort\":20,\"color\":\"#1252AD\",\"enabled\":true}");
        values.put("priority.URGENT", "{\"name\":\"紧急\",\"sort\":40,\"color\":\"#C71F24\",\"enabled\":true}");
        return values;
    }

    private PriorityConfigUpdateRequest changedRequest() {
        return new PriorityConfigUpdateRequest(List.of(
                new PriorityOptionVO("LOW", "低级", 11, "#00AA00", true),
                new PriorityOptionVO("MEDIUM", "普通", 21, "#0000AA", true),
                new PriorityOptionVO("HIGH", "高级", 31, "#AA6600", true),
                new PriorityOptionVO("URGENT", "最高", 41, "#AA0000", true)));
    }

    /** 仅装配本测试所需事务、JDBC Mapper 和服务，避免启动 Redis、安全等无关基础设施。 */
    @Configuration
    @EnableTransactionManagement
    static class TestTransactionConfig {
        @Bean(destroyMethod = "shutdown")
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .generateUniqueName(true)
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        SystemConfigMapper systemConfigMapper(JdbcTemplate jdbcTemplate) {
            return new JdbcSystemConfigMapper(jdbcTemplate);
        }

        @Bean
        AuditLogMapper auditLogMapper(JdbcTemplate jdbcTemplate) {
            return new JdbcAuditLogMapper(jdbcTemplate);
        }

        @Bean
        AuditLogService auditLogService(AuditLogMapper mapper) {
            return new AuditLogServiceImpl(mapper, new SnowflakeIdGenerator());
        }

        @Bean
        PriorityConfigService priorityConfigService(SystemConfigMapper mapper, AuditLogService auditLogService) {
            return new PriorityConfigServiceImpl(mapper, auditLogService, new ObjectMapper());
        }
    }

    /** 基于同一测试数据源的 system_config Mapper，使更新参与 Spring JDBC 事务。 */
    private record JdbcSystemConfigMapper(JdbcTemplate jdbcTemplate) implements SystemConfigMapper {
        @Override
        public List<SystemConfig> findByGroup(String group) {
            return jdbcTemplate.query("SELECT config_key, config_value FROM system_config WHERE config_group=? AND deleted=0",
                    (resultSet, rowNum) -> {
                        SystemConfig config = new SystemConfig();
                        config.setConfigKey(resultSet.getString("config_key"));
                        config.setConfigValue(resultSet.getString("config_value"));
                        return config;
                    }, group);
        }

        @Override
        public int updateValue(String key, String value, Long operatorId) {
            return jdbcTemplate.update("""
                    UPDATE system_config SET config_value=?, update_by=?, update_time=?
                    WHERE config_key=? AND editable=1 AND deleted=0
                    """, value, operatorId, LocalDateTime.now(), key);
        }
    }

    /** 基于同一测试数据源的 audit_log Mapper，真实触发数据库约束异常。 */
    private record JdbcAuditLogMapper(JdbcTemplate jdbcTemplate) implements AuditLogMapper {
        @Override
        public int insert(AuditLog auditLog) {
            return jdbcTemplate.update("""
                    INSERT INTO audit_log(id, operator_id, operation_type, biz_type, biz_id, content,
                      request_ip, user_agent, create_by, update_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, auditLog.getId(), auditLog.getOperatorId(), auditLog.getOperationType(), auditLog.getBizType(),
                    auditLog.getBizId(), auditLog.getContent(), auditLog.getRequestIp(), auditLog.getUserAgent(),
                    auditLog.getCreateBy(), auditLog.getUpdateBy());
        }

        @Override
        public List<AuditLog> search(Long operatorId,
                                     String operationType,
                                     String bizType,
                                     Long bizId,
                                     LocalDateTime dateFrom,
                                     LocalDateTime dateTo,
                                     String keyword) {
            return List.of();
        }
    }
}
