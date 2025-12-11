package com.zsq.winter.local.message.entity;

import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务消息实体命令
 * <p>
 * 该类为领域实体命令对象，用于封装任务消息的核心信息。
 * 包含任务基本信息、通知配置以及业务参数等内容。
 * 支持HTTP、RabbitMQ、Kafka和RocketMQ四种通知方式。
 * </p>
 */
@Data
public class TaskMessageEntityCommand {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务通知类型（http/rabbit_mq/kafka/rocket_mq）
     *
     * @see TaskNotifyEnum
     */
    private String notifyType;

    /**
     * 通知配置（包含HTTP或MQ的具体配置信息）
     */
    private NotifyConfig notifyConfig;

    /**
     * 任务状态（0-待处理，1-处理中，2-已完成，3-失败）
     */
    private Integer status;

    /**
     * 业务参数JSON字符串
     */
    private String parameterJson;

    /**
     * 扩展，保留字段
     */
    private String extension;

    /**
     * 默认构造方法
     */
    public TaskMessageEntityCommand() {
    }

    /**
     * 带参数的构造方法
     *
     * @param taskId         任务ID
     * @param taskName       任务名称
     * @param taskNotifyEnum 任务通知类型枚举
     * @param notifyConfig   通知配置
     * @param parameterJson  业务参数JSON
     */
    public TaskMessageEntityCommand(String taskId, String taskName, TaskNotifyEnum taskNotifyEnum, NotifyConfig notifyConfig, String parameterJson) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.notifyType = taskNotifyEnum.getCode();
        this.notifyConfig = notifyConfig;
        this.status = 0;
        this.parameterJson = parameterJson;
    }

    /**
     * 通知配置类
     * <p>
     * 包含HTTP和各种MQ通知方式的配置信息
     * </p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotifyConfig {
        /**
         * HTTP通知配置
         */
        private HTTP http;

        private MQ mq;

        /**
         * Kafka通知配置
         */
        private Kafka kafka;

        /**
         * RocketMQ通知配置
         */
        private RocketMQ rocketMQ;

        /**
         * MQ配置类
         * <p>
         * 用于配置RabbitMQ的交换机和路由键
         * </p>
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class MQ {
            /**
             * 路由键/队列名称
             */
            private String topic;

            /**
             * 交换机名称
             */
            private String exchange;
        }


        /**
         * HTTP配置类
         * <p>
         * 用于配置HTTP请求的URL、请求方法、内容类型和认证信息
         * </p>
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class HTTP {
            /**
             * HTTP请求URL
             */
            private String url;

            /**
             * HTTP请求方法（GET/POST等）
             */
            private String method;

            /**
             * 内容类型（如application/json）
             */
            private String contentType;

            /**
             * 认证信息（如Bearer Token）
             */
            private String authorization;
        }

        /**
         * Kafka配置类
         * <p>
         * 用于配置Kafka的主题、分区等信息
         * </p>
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Kafka {
            /**
             * Kafka主题名称
             */
            private String topic;
            
            /**
             * 分区键（可选）
             */
            private String partitionKey;
            
            /**
             * 指定分区号（可选）
             */
            private Integer partition;
            
            /**
             * 消息头信息（可选）
             */
            private java.util.Map<String, String> headers;
        }

        /**
         * RocketMQ配置类
         * <p>
         * 用于配置RocketMQ的主题、标签等信息
         * </p>
         */
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class RocketMQ {
            /**
             * RocketMQ主题名称
             */
            private String topic;
            
            /**
             * 消息标签（可选）
             */
            private String tag;
            
            /**
             * 消息键（可选）
             */
            private String key;
            
            /**
             * 延迟级别（可选）
             */
            private Integer delayLevel;
            
            /**
             * 生产者组（可选）
             */
            private String producerGroup;
        }
    }

}
