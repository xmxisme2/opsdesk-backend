package com.opsdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpsDesk 后端启动入口。
 *
 * <p>当前阶段只初始化工程骨架和公共能力，业务模块会按文档约定逐步补充。</p>
 */
@SpringBootApplication
@EnableScheduling
public class OpsDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsDeskApplication.class, args);
    }
}
