package com.zsq.winter.local.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地任务消息配置属性类
 * <p>
 * 支持配置多个任务组，每个组包含：
 * </p>
 * <ul>
 *   <li>groupId: 任务组标识</li>
 *   <li>houseNumbers: 扫描的门牌号列表（如 1,2,3）</li>
 *   <li>cron: 执行调度的 cron 表达式（可选）</li>
 *   <li>fixedDelayMs: 固定延迟毫秒（可选，和 cron 二选一）</li>
 *   <li>limit: 每次拉取的最大任务条数（默认 100）</li>
 * </ul>
 * 
 * <p><b>配置示例：</b></p>
 * <pre>
 * winter-local-task-message:
 *   groups:
 *     - group-id: group1
 *       house-numbers: [0, 1, 2]
 *       cron: "0/10 * * * * ?"
 *       limit: 50
 *     - group-id: group2
 *       house-numbers: [3, 4, 5]
 *       fixed-delay-ms: 5000
 *       limit: 100
 * </pre>
 * 
 * 
 * @since 2025/11/16 15:36
 */
@Data
@ConfigurationProperties(prefix = "winter-local-task-message", ignoreInvalidFields = true)
public class LocalTaskMessageAutoProperties {

    /**
     * 任务组配置列表
     */
    private List<TaskGroupConfig> groups = new ArrayList<>();

    /**
     * 任务组配置类
     * <p>
     * 单个任务组的配置信息，包括门牌号、调度表达式和批量大小。
     * </p>
     */
    @Data
    public static class TaskGroupConfig {
        /**
         * 任务组ID或名称，用于日志区分
         */
        private String groupId = "default";

        /**
         * 扫描的门牌号列表，例如 [1,2,3]
         */
        private List<Integer> houseNumbers = new ArrayList<>();

        /**
         * 调度 cron 表达式，例如："0/10 * * * * ?" 表示每10秒
         */
        private String cron;

        /**
         * 固定延迟毫秒；若配置此项则使用固定延迟调度
         */
        private Long fixedDelayMs;

        /**
         * 每次批量处理限制条数
         */
        private Integer limit = 100;
    }

}
