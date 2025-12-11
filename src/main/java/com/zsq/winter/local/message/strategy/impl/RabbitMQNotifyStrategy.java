package com.zsq.winter.local.message.strategy.impl;


import cn.hutool.json.JSONUtil;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ通知策略实现类
 * <p>
 * 实现RabbitMQ方式的任务消息通知。
 * 通过RabbitMQ消息队列向目标系统发送通知，支持配置交换机和路由键。
 * 通知成功后更新任务状态为成功，失败后更新为失败。
 * </p>
 * 
 * 
 */
@Slf4j
public class RabbitMQNotifyStrategy implements INotifyStrategy {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 任务消息仓储服务，用于更新任务状态
     */
    private final ILocalTaskMessageService repository;

    /**
     * 构造方法
     *
     * @param rabbitTemplate RabbitMQ事件发布类
     * @param repository 任务消息仓储服务
     */
    public RabbitMQNotifyStrategy(RabbitTemplate rabbitTemplate, ILocalTaskMessageService repository) {
        this.rabbitTemplate = rabbitTemplate;
        this.repository = repository;
    }

    /**
     * 执行RabbitMQ通知
     * <p>
     * 通过RabbitMQ发送消息到指定的交换机和路由键。
     * 成功后更新任务状态为2（已完成），失败后更新为3（失败）。
     * </p>
     * 
     * @param command 任务消息实体命令
     * @return 通知结果
     * @throws Exception RabbitMQ发送失败时抛出异常
     */
    @Override
    public String notify(TaskMessageEntityCommand command) throws Exception {
        try {
            TaskMessageEntityCommand.NotifyConfig.MQ mq = command.getNotifyConfig().getMq();
            publish(mq.getExchange(), mq.getTopic(), command.getParameterJson());
             // 通知成功，更新状态为成功
            repository.updateTaskStatusToSuccess(command.getTaskId());
            return "success";
        } catch (Exception e) {
            log.error("rabbitmq notify error {}", JSONUtil.toJsonStr(command), e);
            // 通知失败，更新状态为失败
            repository.updateTaskStatusToFailed(command.getTaskId());
            throw e;
        }
    }

    public void publish(String exchange, String routingKey, String message) {
        try {
            if (null == rabbitTemplate){
                log.error("应用服务方，尚未配置 RabbitMQ Template 不能完成 MQ 发送");
                return;
            }

            rabbitTemplate.convertAndSend(exchange, routingKey, message, m -> {
                // 持久化消息配置
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return m;
            });
        } catch (Exception e) {
            log.error("发送MQ消息失败 exchange:{} routingKey:{} message:{}", exchange, routingKey, message, e);
            throw e;
        }
    }

    @Override
    public void execute(Object... params) {

    }

    @Override
    public TaskNotifyEnum getStrategyType() {
        return TaskNotifyEnum.RABBIT_MQ;
    }
}
