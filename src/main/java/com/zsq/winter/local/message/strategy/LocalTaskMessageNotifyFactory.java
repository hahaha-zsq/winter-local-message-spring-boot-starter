package com.zsq.winter.local.message.strategy;
import com.zsq.winter.design.strategy.AbstractStrategyFactory;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 本地任务消息通知工厂实现类
 * <p>
 * 实现任务消息通知的统一入口，使用策略模式。
 * 在初始化时将所有通知策略注册到Map中，根据通知类型路由到对应的策略。
 * 支持用户自定义策略的动态注册。
 * </p>
 */
@Slf4j
public class LocalTaskMessageNotifyFactory extends AbstractStrategyFactory<TaskNotifyEnum, INotifyStrategy> {

    /**
     * 构造方法
     *
     * @param notifyStrategyList 所有通知策略列表
     */
    public LocalTaskMessageNotifyFactory(List<INotifyStrategy> notifyStrategyList) {
        super(INotifyStrategy.class, notifyStrategyList);
    }

}
