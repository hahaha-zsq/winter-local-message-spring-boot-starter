package com.zsq.winter.local.message.config;

import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.impl.KafkaNotifyStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka通知策略自动配置类
 * <p>
 * 只有当Kafka相关类存在于classpath时才会加载此配置
 * </p>
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaNotifyAutoConfig {

    /**
     * 创建Kafka通知策略
     * <p>
     * required=false避免用户未配置Kafka时报错。
     * 实现Kafka方式的任务消息通知。
     * </p>
     *
     * @param kafkaTemplate Kafka模板
     * @param localTaskMessageService 任务消息仓储服务
     * @return Kafka通知策略Bean
     */
    @Bean
    public INotifyStrategy kafkaNotifyStrategy(
            @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
            ILocalTaskMessageService localTaskMessageService) {
        return new KafkaNotifyStrategy(kafkaTemplate, localTaskMessageService);
    }
}