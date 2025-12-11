package com.zsq.winter.local.message.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务消息数据库表持久化对象
 * <p>
 * 该类为数据库表local_task_message的PO对象，用于存储本地任务消息数据。
 * 通过门牌号（houseNumber）进行分库分表，支持多种通知方式。
 * </p>
 * 
 * 
 */
@Data
public class TaskMessagePO {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 通知类型
     */
    private String notifyType;

    /**
     * 通知配置（JSON格式，包含mqTopic和url等信息）
     */
    private String notifyConfig;

    /**
     * 状态（0-待处理，1-处理中，2-已完成，3-失败）
     */
    private Integer status;

    /**
     * 参数JSON
     */
    private String parameterJson;

    /**
     * 门牌号
     */
    private Integer houseNumber;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
