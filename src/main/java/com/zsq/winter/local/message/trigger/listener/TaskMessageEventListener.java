package com.zsq.winter.local.message.trigger.listener;


import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.event.SpringTaskMessageEvent;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.LocalTaskMessageNotifyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * 任务消息事件监听器
 * <p>
 * 监听Spring发布的SpringTaskMessageEvent事件，异步处理任务消息通知。
 * 当AOP切面或定时任务发布事件后，该监听器会异步执行通知操作。
 * 使用@Async注解实现异步处理，不阻塞主线程。
 * </p>
 * 
 * <p><b>处理流程：</b></p>
 * <ol>
 *   <li>接收SpringTaskMessageEvent事件</li>
 *   <li>调用通知工厂执行通知（HTTP或RabbitMQ）</li>
 *   <li>根据通知结果更新任务状态</li>
 * </ol>
 * 
 * 
 * @see SpringTaskMessageEvent
 * @see LocalTaskMessageNotifyFactory
 */
@Slf4j
public class TaskMessageEventListener {

    private final LocalTaskMessageNotifyFactory factory;

    /**
     * 构造方法
     *
     * @param factory 任务消息通知工厂
     */
    public TaskMessageEventListener(LocalTaskMessageNotifyFactory factory) {
        this.factory = factory;
    }

    @EventListener
    @Async // 默认使用Spring创建ThreadPoolTaskExecutor，我们已经在配置中重新覆盖了这个线程池
    public void handleTaskMessageEvent(SpringTaskMessageEvent event) {
        try {
            TaskMessageEntityCommand command = event.getTaskMessageEntityCommand();
            log.info("收到任务消息事件 - 消息内容: {}, 事件时间戳: {}", command, event.getTimestamp());
            String notifyType = command.getNotifyType();
            // 获取通知策略
            INotifyStrategy notifyStrategy = factory.getStrategy(notifyType, TaskNotifyEnum.class);
            // 执行通知操作
            String notify = notifyStrategy.notify(command);
            log.info("收到任务消息事件 - 通知结果: {}", notify);
        } catch (Exception e) {
            log.error("处理任务消息事件失败 - 消息: {}, 错误: {}",
                    event.getTaskMessageEntityCommand(), e.getMessage(), e);
        }
    }

}
