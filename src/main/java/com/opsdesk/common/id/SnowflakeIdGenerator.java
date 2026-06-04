package com.opsdesk.common.id;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>当前数据库主键由后端显式写入，使用本生成器保证各业务表新增数据都有稳定的 Long ID。</p>
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1782576000000L;
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = MACHINE_ID_BITS + SEQUENCE_BITS;

    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator() {
        this.machineId = new SecureRandom().nextInt((int) (1L << MACHINE_ID_BITS));
    }

    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();
        if (currentTimestamp < lastTimestamp) {
            currentTimestamp = waitNextMillis(lastTimestamp);
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long previousTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= previousTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
