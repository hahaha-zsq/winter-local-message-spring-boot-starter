package com.zsq.winter.local.message;

import cn.hutool.json.JSONUtil;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.event.SpringTaskMessageEvent;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 本地任务消息处理服务实现类
 * <p>
 * 实现任务消息的受理处理逻辑，是AOP切面后的主要处理入口。
 * 采用先入库再发送的模式，保证消息不丢失。
 * </p>
 * 
 * 
 */
@Slf4j
public class LocalTaskMessageTemplate {

    private final ILocalTaskMessageService repository;
    /**
     * Spring事件发布器
     */
    private final ApplicationEventPublisher eventPublisher;

    public LocalTaskMessageTemplate(ApplicationEventPublisher eventPublisher,
                                    ILocalTaskMessageService repository) {
        this.repository = repository;
        this.eventPublisher =eventPublisher;
    }

    /**
     * 接受任务消息
     * <p>
     * 1. 保存任务消息
     * 2. 发布事件消息
     * </p>
     *
     * @param command 任务消息命令
     */
    public void acceptTaskMessage(TaskMessageEntityCommand command) {
        try {
            log.info("受理任务消息: {}", command);

            // 1. 保存任务消息
            repository.saveTaskMessage(command);

            // 2. 发布事件消息
            // 构建事件
            SpringTaskMessageEvent springTaskMessageEvent = new SpringTaskMessageEvent(this, command);
            // 发布事件
            eventPublisher.publishEvent(springTaskMessageEvent);

        } catch (Exception e) {
            log.error("受理任务消息执行失败 {}", JSONUtil.toJsonStr(command), e);

            throw new RuntimeException(e);
        }
    }

}
