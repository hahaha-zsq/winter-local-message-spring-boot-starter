package com.zsq.winter.local.message.strategy.impl;

import cn.hutool.json.JSONUtil;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Kafka通知策略实现
 * <p>
 * 通过Kafka消息队列发送任务消息通知。
 * 支持分区键、指定分区和消息头配置。
 * </p>
 */
@Slf4j
public class KafkaNotifyStrategy implements INotifyStrategy {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 任务消息仓储服务，用于更新任务状态
     */
    private final ILocalTaskMessageService repository;

    /**
     * 构造方法
     *
     * @param kafkaTemplate Kafka模板
     * @param repository 任务消息仓储服务
     */
    public KafkaNotifyStrategy(KafkaTemplate<String, String> kafkaTemplate, ILocalTaskMessageService repository) {
        this.kafkaTemplate = kafkaTemplate;
        this.repository = repository;
    }

    /**
     * 执行Kafka通知
     * <p>
     * 通过Kafka发送消息到指定的主题。
     * 成功后更新任务状态为2（已完成），失败后更新为3（失败）。
     * </p>
     * 
     * @param command 任务消息实体命令
     * @return 通知结果
     * @throws Exception Kafka发送失败时抛出异常
     */
    @Override
    public String notify(TaskMessageEntityCommand command) throws Exception {
        try {
            TaskMessageEntityCommand.NotifyConfig.Kafka kafka = command.getNotifyConfig().getKafka();
            
            // 发送Kafka消息
            sendKafkaMessage(kafka, command.getParameterJson(), command.getTaskId());
            
            // 通知成功，更新状态为成功
            repository.updateTaskStatusToSuccess(command.getTaskId());
            log.info("Kafka通知成功 - TaskId: {}, Topic: {}", command.getTaskId(), kafka.getTopic());
            
            return "success";
        } catch (Exception e) {
            log.error("Kafka通知失败 - TaskId: {}, 配置: {}", 
                command.getTaskId(), JSONUtil.toJsonStr(command.getNotifyConfig()), e);
            // 通知失败，更新状态为失败
            repository.updateTaskStatusToFailed(command.getTaskId());
            throw e;
        }
    }

    /**
     * 发送Kafka消息
     *
     * @param kafka Kafka配置
     * @param message 消息内容
     * @param taskId 任务ID
     */
    private void sendKafkaMessage(TaskMessageEntityCommand.NotifyConfig.Kafka kafka, String message, String taskId) {
        try {
            if (null == kafkaTemplate) {
                log.error("应用服务方，尚未配置 Kafka Template 不能完成 Kafka 发送");
                return;
            }

            // 根据配置选择发送方式
            if (kafka.getPartition() != null) {
                // 发送到指定分区
                kafkaTemplate.send(kafka.getTopic(), kafka.getPartition(), kafka.getPartitionKey(), message)
                    .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                        @Override
                        public void onSuccess(SendResult<String, String> result) {
                            log.info("Kafka消息发送成功 - TaskId: {}, Topic: {}, Partition: {}, Offset: {}", 
                                taskId, kafka.getTopic(), 
                                result.getRecordMetadata().partition(), 
                                result.getRecordMetadata().offset());
                        }

                        @Override
                        public void onFailure(Throwable ex) {
                            log.error("Kafka消息发送失败 - TaskId: {}, Topic: {}", 
                                taskId, kafka.getTopic(), ex);
                        }
                    });
            } else if (kafka.getPartitionKey() != null) {
                // 使用分区键发送
                kafkaTemplate.send(kafka.getTopic(), kafka.getPartitionKey(), message);
                log.info("Kafka消息发送成功 - TaskId: {}, Topic: {}, PartitionKey: {}", 
                    taskId, kafka.getTopic(), kafka.getPartitionKey());
            } else {
                // 普通发送
                kafkaTemplate.send(kafka.getTopic(), message);
                log.info("Kafka消息发送成功 - TaskId: {}, Topic: {}", taskId, kafka.getTopic());
            }

        } catch (Exception e) {
            log.error("发送Kafka消息失败 - Topic: {}, Message: {}", kafka.getTopic(), message, e);
            throw e;
        }
    }


    @Override
    public void execute(Object... params) {

    }

    @Override
    public TaskNotifyEnum getStrategyType() {
        return TaskNotifyEnum.KAFKA;
    }
}