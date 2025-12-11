package com.zsq.winter.local.message.strategy.impl;

import cn.hutool.json.JSONUtil;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * RocketMQ通知策略实现
 * <p>
 * 通过RocketMQ消息队列发送任务消息通知。
 * 支持延迟消息、消息标签和消息键配置。
 * </p>
 */
@Slf4j
public class RocketMQNotifyStrategy implements INotifyStrategy {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 任务消息仓储服务，用于更新任务状态
     */
    private final ILocalTaskMessageService repository;

    /**
     * 构造方法
     *
     * @param rocketMQTemplate RocketMQ模板
     * @param repository 任务消息仓储服务
     */
    public RocketMQNotifyStrategy(RocketMQTemplate rocketMQTemplate, ILocalTaskMessageService repository) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.repository = repository;
    }

    /**
     * 执行RocketMQ通知
     * <p>
     * 通过RocketMQ发送消息到指定的主题。
     * 成功后更新任务状态为2（已完成），失败后更新为3（失败）。
     * </p>
     * 
     * @param command 任务消息实体命令
     * @return 通知结果
     * @throws Exception RocketMQ发送失败时抛出异常
     */
    @Override
    public String notify(TaskMessageEntityCommand command) throws Exception {
        try {
            TaskMessageEntityCommand.NotifyConfig.RocketMQ rocketMQ = command.getNotifyConfig().getRocketMQ();
            
            // 发送RocketMQ消息
            sendRocketMQMessage(rocketMQ, command.getParameterJson(), command.getTaskId());
            
            // 通知成功，更新状态为成功
            repository.updateTaskStatusToSuccess(command.getTaskId());
            log.info("RocketMQ通知成功 - TaskId: {}, Topic: {}", command.getTaskId(), rocketMQ.getTopic());
            
            return "success";
        } catch (Exception e) {
            log.error("RocketMQ通知失败 - TaskId: {}, 配置: {}", 
                command.getTaskId(), JSONUtil.toJsonStr(command.getNotifyConfig()), e);
            // 通知失败，更新状态为失败
            repository.updateTaskStatusToFailed(command.getTaskId());
            throw e;
        }
    }

    /**
     * 发送RocketMQ消息
     *
     * @param rocketMQ RocketMQ配置
     * @param messageContent 消息内容
     * @param taskId 任务ID
     */
    private void sendRocketMQMessage(TaskMessageEntityCommand.NotifyConfig.RocketMQ rocketMQ, 
                                   String messageContent, String taskId) {
        try {
            if (null == rocketMQTemplate) {
                log.error("应用服务方，尚未配置 RocketMQ Template 不能完成 RocketMQ 发送");
                return;
            }

            // 构建目标地址
            String destination = buildDestination(rocketMQ);
            
            // 构建消息
            Message<String> message = MessageBuilder
                .withPayload(messageContent)
                .setHeader("taskId", taskId)
                .build();

            // 根据是否有延迟级别选择发送方式
            if (rocketMQ.getDelayLevel() != null && rocketMQ.getDelayLevel() > 0) {
                // 发送延迟消息
                rocketMQTemplate.syncSend(destination, message, 3000, rocketMQ.getDelayLevel());
                log.info("RocketMQ延迟消息发送成功 - TaskId: {}, Topic: {}, DelayLevel: {}", 
                    taskId, rocketMQ.getTopic(), rocketMQ.getDelayLevel());
            } else {
                // 发送普通消息
                rocketMQTemplate.syncSend(destination, message);
                log.info("RocketMQ消息发送成功 - TaskId: {}, Topic: {}", taskId, rocketMQ.getTopic());
            }

        } catch (Exception e) {
            log.error("发送RocketMQ消息失败 - Topic: {}, Message: {}", rocketMQ.getTopic(), messageContent, e);
            throw e;
        }
    }

    /**
     * 构建RocketMQ目标地址
     * 
     * @param rocketMQ RocketMQ配置
     * @return 目标地址 (topic:tag格式)
     */
    private String buildDestination(TaskMessageEntityCommand.NotifyConfig.RocketMQ rocketMQ) {
        StringBuilder destination = new StringBuilder(rocketMQ.getTopic());
        
        if (rocketMQ.getTag() != null && !rocketMQ.getTag().trim().isEmpty()) {
            destination.append(":").append(rocketMQ.getTag());
        }
        
        return destination.toString();
    }


    @Override
    public void execute(Object... params) {

    }

    @Override
    public TaskNotifyEnum getStrategyType() {
        return TaskNotifyEnum.ROCKET_MQ;
    }
}