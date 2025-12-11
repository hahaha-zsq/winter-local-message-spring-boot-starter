package com.zsq.winter.local.message.config;

import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.impl.RabbitMQNotifyStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ通知策略自动配置类
 * <p>
 * 只有当RabbitMQ相关类存在于classpath时才会加载此配置
 * </p>
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMQNotifyAutoConfig {

    /**
     * 创建RabbitMQ通知策略
     * <p>
     * required=false避免用户未配置RabbitMQ时报错。
     * 实现RabbitMQ方式的任务消息通知。
     * </p>
     *
     * @param rabbitTemplate RabbitMQ模板
     * @param localTaskMessageService 任务消息仓储服务
     * @return RabbitMQ通知策略Bean
     */
    @Bean
    public INotifyStrategy rabbitMQNotifyStrategy(
            @Autowired(required = false) RabbitTemplate rabbitTemplate,
            ILocalTaskMessageService localTaskMessageService) {
        return new RabbitMQNotifyStrategy(rabbitTemplate, localTaskMessageService);
    }
}