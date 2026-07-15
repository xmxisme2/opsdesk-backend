package com.opsdesk.ticket.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 工单分类活动记录唯一索引迁移回归测试。 */
class TicketCategorySchemaTest {

    @Test
    void schemaShouldUniquelyConstrainOnlyActiveSiblingNames() throws IOException {
        String schema = Files.readString(Path.of("sql/01_schema.sql"));
        String migration = Files.readString(Path.of("sql/05_migrate_ticket_category_active_unique.sql"));

        assertThat(schema).contains("active_parent_id BIGINT GENERATED ALWAYS AS")
                .contains("active_name VARCHAR(128) GENERATED ALWAYS AS")
                .contains("UNIQUE KEY uk_ticket_category_active_name (active_parent_id, active_name)")
                .doesNotContain("uk_ticket_category_name_deleted");
        assertThat(migration).contains("uk_ticket_category_active_name")
                .contains("COALESCE(parent_id, 0)")
                .contains("CASE WHEN deleted = 0 THEN name ELSE NULL END");
    }
}
