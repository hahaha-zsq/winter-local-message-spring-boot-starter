package com.zsq.winter.local.message.config;

import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.impl.RocketMQNotifyStrategy;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ通知策略自动配置类
 * <p>
 * 只有当RocketMQ相关类存在于classpath时才会加载此配置
 * </p>
 */
@Configuration
@ConditionalOnClass(RocketMQTemplate.class)
public class RocketMQNotifyAutoConfig {

    /**
     * 创建RocketMQ通知策略
     * <p>
     * required=false避免用户未配置RocketMQ时报错。
     * 实现RocketMQ方式的任务消息通知。
     * </p>
     *
     * @param rocketMQTemplate RocketMQ模板
     * @param localTaskMessageService 任务消息仓储服务
     * @return RocketMQ通知策略Bean
     */
    @Bean
    public INotifyStrategy rocketMQNotifyStrategy(
            @Autowired(required = false) RocketMQTemplate rocketMQTemplate,
            ILocalTaskMessageService localTaskMessageService) {
        return new RocketMQNotifyStrategy(rocketMQTemplate, localTaskMessageService);
    }
}