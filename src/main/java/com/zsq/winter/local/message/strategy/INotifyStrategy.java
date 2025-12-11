package com.zsq.winter.local.message.strategy;


import com.zsq.winter.design.strategy.BaseStrategy;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;

/**
 * 通知策略接口
 * <p>
 * 定义任务消息通知的策略接口，使用策略模式支持多种通知方式。
 * 各种通知方式（HTTP、RabbitMQ等）需要实现该接口。
 * </p>
 * 
 * 
 * @see TaskNotifyEnum
 */
public interface INotifyStrategy extends BaseStrategy<TaskNotifyEnum> {
    /**
     * 执行通知
     * <p>
     * 根据任务消息命令中的配置信息，执行具体的通知操作。
     * </p>
     * 
     * @param command 任务消息实体命令
     * @return 通知结果
     * @throws Exception 通知失败时抛出异常
     */
    String notify(TaskMessageEntityCommand command) throws Exception;

}
