package com.zsq.winter.local.message.enums;

import com.zsq.winter.design.strategy.BaseEnum;
import lombok.Getter;

/**
 * 任务通知类型枚举
 * <p>
 * 定义本地任务消息支持的通知方式类型。
 * 目前支持HTTP接口调用、RabbitMQ、Kafka和RocketMQ消息队列四种通知方式。
 * </p>
 *
 * 
 * @since 2025/11/12 08:00
 */
@Getter
public enum TaskNotifyEnum implements BaseEnum {

    /**
     * HTTP通知方式：通过HTTP接口调用进行通知
     */
    HTTP("http", "HTTP通知"),
    
    /**
     * RabbitMQ通知方式：通过RabbitMQ消息队列进行通知
     */
    RABBIT_MQ("rabbit_mq", "RabbitMQ通知"),
    
    /**
     * Kafka通知方式：通过Kafka消息队列进行通知
     */
    KAFKA("kafka", "Kafka通知"),
    
    /**
     * RocketMQ通知方式：通过RocketMQ消息队列进行通知
     */
    ROCKET_MQ("rocket_mq", "RocketMQ通知"),
    ;
    private final String code;

    private final String desc;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 构造方法
     *
     * @param code 通知类型代码
     * @param desc 通知类型描述
     */
    TaskNotifyEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
