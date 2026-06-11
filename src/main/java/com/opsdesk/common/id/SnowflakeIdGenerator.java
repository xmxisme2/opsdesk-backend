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

    /** 自定义纪元毫秒时间戳：用于缩短生成 ID 的时间位长度，保持同一项目内稳定。 */
    private static final long EPOCH = 1704067200000L; // 2024-01-01T00:00:00Z

    /** 机器位长度：预留 10 位支持不同实例生成不同机器号。 */
    private static final long MACHINE_ID_BITS = 10L;

    /** 序列位长度：同一毫秒内最多生成 4096 个 ID。 */
    private static final long SEQUENCE_BITS = 12L;

    /** 单毫秒最大序列号：序列耗尽时等待下一毫秒。 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器号左移位数：低位留给毫秒内自增序列。 */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数：低位依次放机器号和序列号。 */
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
