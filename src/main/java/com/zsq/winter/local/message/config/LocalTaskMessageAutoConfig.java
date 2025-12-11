package com.zsq.winter.local.message.config;

import com.zsq.winter.local.message.LocalTaskMessageTemplate;
import com.zsq.winter.local.message.config.aop.LocalTaskMessageAop;
import com.zsq.winter.local.message.dao.ITaskMessageDao;
import com.zsq.winter.local.message.dao.TaskMessageDaoImpl;
import com.zsq.winter.local.message.service.*;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.LocalTaskMessageNotifyFactory;
import com.zsq.winter.local.message.strategy.impl.HTTPNotifyStrategy;
import com.zsq.winter.local.message.trigger.job.TaskMessageEventJob;
import com.zsq.winter.local.message.trigger.listener.TaskMessageEventListener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;


/**
 * 本地任务消息自动配置类
 * <p>
 * Spring Boot Starter的自动配置类，负责自动装配本地任务消息的核心组件。
 * 启用异步支持和定时任务调度，扫描并加载相关Bean。
 * 配置了专用的线程池调度器用于定时任务执行。
 * </p>
 *
 * @see LocalTaskMessageAutoProperties
 * @see org.springframework.boot.autoconfigure.EnableAutoConfiguration
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(value = {LocalTaskMessageAutoProperties.class})
@Import({KafkaNotifyAutoConfig.class, RocketMQNotifyAutoConfig.class, RabbitMQNotifyAutoConfig.class})
public class LocalTaskMessageAutoConfig {
    /**
     * 创建任务消息调度器
     * <p>
     * 配置专用的线程池调度器，用于执行定时任务扫描。
     * 设置池大小为2，支持多个任务组并发调度。
     * </p>
     *
     * @return 线程池调度器Bean
     */
    @Bean("taskMessageScheduler")
    public ThreadPoolTaskScheduler taskMessageScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("TaskMessageScheduler-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 创建事务模板
     * <p>
     * 配置事务管理模板，供AOP切面使用。
     * 用于在没有事务的情况下开启新事务。
     * </p>
     *
     * @param transactionManager 事务管理器
     * @return 事务模板Bean
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * 创建任务消息DAO实现
     * <p>
     * 负责任务消息的数据库操作。
     * </p>
     *
     * @param dataSource 数据源
     * @return 任务消息DAO Bean
     */
    @Bean
    public ITaskMessageDao taskMessageDao(DataSource dataSource) {
        return new TaskMessageDaoImpl(dataSource);
    }

    /**
     * 创建任务消息仓储服务
     * <p>
     * 负责任务消息的仓储层操作。
     * </p>
     * 允许使用者自定义实现
     * 如果用户已经配置了自己的 ILocalTaskMessageService 实现，就用用户的；否则用这个默认实现
     *
     * @param taskMessageDao 任务消息DAO
     * @return 任务消息仓储服务Bean
     */
    @Bean
    @ConditionalOnMissingBean // 默认检查容器中是否存在 ILocalTaskMessageService 类型的 Bean
    public ILocalTaskMessageService localTaskMessageService(ITaskMessageDao taskMessageDao) {
        return new LocalTaskMessageServiceImpl(taskMessageDao);
    }

    /**
     * 创建任务消息处理服务
     * <p>
     * AOP切面后的主要处理入口。
     * </p>
     *
     * @param eventPublisher          Spring事件发布器
     * @param localTaskMessageService 任务消息仓储服务
     * @return 任务消息处理服务Bean
     */
    @Bean
    public LocalTaskMessageTemplate localTaskMessageTemplate(
            ApplicationEventPublisher eventPublisher,
            ILocalTaskMessageService localTaskMessageService) {
        return new LocalTaskMessageTemplate(eventPublisher, localTaskMessageService);
    }


    /**
     * 创建HTTP通知策略
     * <p>
     * 实现HTTP方式的任务消息通知。
     * </p>
     *
     * @param localTaskMessageService 任务消息仓储服务
     * @return HTTP通知策略Bean
     */
    @Bean
    public INotifyStrategy httpNotifyStrategy(
            ILocalTaskMessageService localTaskMessageService) {
        return new HTTPNotifyStrategy(localTaskMessageService);
    }

    /**
     * 创建任务消息通知工厂
     * <p>
     * 使用策略模式，根据通知类型路由到对应的策略。
     * 支持用户自定义策略的自动发现和注册。
     * </p>
     *
     * @param notifyStrategyList 所有通知策略列表
     * @return 任务消息通知工厂Bean
     */
    @Bean
    public LocalTaskMessageNotifyFactory localTaskMessageNotifyFactory(List<INotifyStrategy> notifyStrategyList) {
        return new LocalTaskMessageNotifyFactory(notifyStrategyList);
    }

    /**
     * 创建任务消息AOP切面
     * <p>
     * 拦截@LocalTaskMessage注解的方法。
     * </p>
     *
     * @param localTaskMessageTemplate 任务消息处理服务
     * @param transactionTemplate      事务模板
     * @return 任务消息AOP切面Bean
     */
    @Bean
    public LocalTaskMessageAop localTaskMessageAop(
            LocalTaskMessageTemplate localTaskMessageTemplate,
            TransactionTemplate transactionTemplate) {
        return new LocalTaskMessageAop(localTaskMessageTemplate, transactionTemplate);
    }

    /**
     * 创建任务消息事件监听器
     * <p>
     * 监听SpringTaskMessageEvent事件，异步处理任务消息通知。
     * </p>
     *
     * @param localTaskMessageNotifyFactory 任务消息通知工厂
     * @return 任务消息事件监听器Bean
     */
    @Bean
    public TaskMessageEventListener taskMessageEventListener(LocalTaskMessageNotifyFactory localTaskMessageNotifyFactory) {
        return new TaskMessageEventListener(localTaskMessageNotifyFactory);
    }

    /**
     * 创建任务消息定时任务
     * <p>
     * 动态加载配置的任务组，扫描并发布任务消息。
     * </p>
     *
     * @param properties                    自动配置属性
     * @param scheduler                     任务调度器
     * @param localTaskMessageNotifyFactory 任务消息通知工厂
     * @param localTaskMessageService       任务消息仓储服务
     * @return 任务消息定时任务Bean
     */
    @Bean
    public TaskMessageEventJob taskMessageEventJob(
            LocalTaskMessageAutoProperties properties,
            ThreadPoolTaskScheduler scheduler,
            LocalTaskMessageNotifyFactory localTaskMessageNotifyFactory,
            ILocalTaskMessageService localTaskMessageService) {
        return new TaskMessageEventJob(properties, scheduler, localTaskMessageNotifyFactory, localTaskMessageService);
    }

}
