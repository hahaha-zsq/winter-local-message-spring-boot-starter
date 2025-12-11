package com.zsq.winter.local.message.trigger.job;


import com.zsq.winter.local.message.config.LocalTaskMessageAutoProperties;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import com.zsq.winter.local.message.strategy.LocalTaskMessageNotifyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务消息事件定时任务
 * <p>
 * 动态加载配置的任务组，扫描指定门牌号的任务消息并发布通知。
 * 支持多个任务组并发执行，每个组可独立配置扫描的门牌号和调度策略。
 * 实现了基于ID的增量扫描，避免重复处理。
 * </p>
 * 
 * <p><b>功能特点：</b></p>
 * <ul>
 *   <li>支持cron表达式和固定延迟两种调度方式</li>
 *   <li>每个任务组独立维护lastId，实现增量扫描</li>
 *   <li>支持配置批量大小，控制单次处理量</li>
 *   <li>自动重试处理失败的任务（status=0或3）</li>
 * </ul>
 * 
 * 
 * @see LocalTaskMessageAutoProperties
 * @see LocalTaskMessageNotifyFactory
 */
@Slf4j
public class TaskMessageEventJob {

    /**
     * 记录每个任务组最近一次处理的最大ID
     * <p>
     * key: 任务组ID（groupId）
     * value: 该任务组已处理的最大消息ID，使用AtomicLong保证线程安全
     * </p>
     * 作用：实现增量扫描，避免重复处理已处理过的消息
     */
    private final Map<String, AtomicLong> groupLastIdMap = new ConcurrentHashMap<>();

    /**
     * 自动配置属性，包含所有任务组的配置信息
     */
    private final LocalTaskMessageAutoProperties properties;

    /**
     * 线程池调度器，用于执行定时任务
     */
    private final ThreadPoolTaskScheduler scheduler;

    /**
     * 任务消息通知工厂，负责根据通知类型选择对应的通知策略
     */
    private final LocalTaskMessageNotifyFactory factory;

    /**
     * 任务消息仓储服务，提供数据查询功能
     */
    private final ILocalTaskMessageService localTaskMessageService;

    /**
     * 构造方法
     *
     * @param properties 自动配置属性
     * @param scheduler 线程池调度器
     * @param factory 任务消息通知工厂
     * @param localTaskMessageService 任务消息仓储服务
     */
    public TaskMessageEventJob(LocalTaskMessageAutoProperties properties, ThreadPoolTaskScheduler scheduler, LocalTaskMessageNotifyFactory factory, ILocalTaskMessageService localTaskMessageService) {
        this.properties = properties;
        this.scheduler = scheduler;
        this.factory = factory;
        this.localTaskMessageService = localTaskMessageService;
    }

    /**
     * 初始化方法
     * <p>
     * 在Bean创建完成后自动执行，负责：
     * 1. 读取所有任务组配置
     * 2. 为每个任务组启动独立的定时调度任务
     * </p>
     * 
     * 如果未配置任务组，则跳过初始化
     */
    @PostConstruct
    public void init() {
        // 获取所有任务组配置
        List<LocalTaskMessageAutoProperties.TaskGroupConfig> groups = properties.getGroups();
        if (groups == null || groups.isEmpty()) {
            log.info("TaskMessageEventJob 未配置任务组，跳过调度初始化");
            return;
        }

        // 为每个任务组启动调度任务
        for (LocalTaskMessageAutoProperties.TaskGroupConfig group : groups) {
            scheduleGroup(group);
        }
    }

    /**
     * 为指定任务组启动定时调度
     * <p>
     * 执行流程：
     * 1. 验证任务组配置的有效性
     * 2. 初始化该任务组的起始扫描ID
     * 3. 创建定时任务（Runnable）
     * 4. 根据配置选择cron或固定延迟方式进行调度
     * </p>
     *
     * @param group 任务组配置
     */
    private void scheduleGroup(LocalTaskMessageAutoProperties.TaskGroupConfig group) {
        String groupId = group.getGroupId();
        List<Integer> houseNumbers = group.getHouseNumbers();
        
        // 验证门牌号配置
        if (houseNumbers == null || houseNumbers.isEmpty()) {
            log.warn("任务组 [{}] 未配置 houseNumbers，跳过该组调度", groupId);
            return;
        }

        // 初始化该任务组的lastId（上次扫描到的最大ID）
        // 使用computeIfAbsent确保每个groupId只初始化一次
        groupLastIdMap.computeIfAbsent(groupId, k -> {
            // 查询该门牌号范围内的最小ID作为起始点
            Long minId = localTaskMessageService.selectMinIdByHouseNumber(houseNumbers);
            long startId = (minId == null ? 0L : minId);
            log.info("任务组 [{}] 初始化起始ID为 {}，houseNumbers={}", groupId, startId, houseNumbers);
            // 返回AtomicLong保证并发安全
            return new AtomicLong(startId);
        });

        /**
         * 定时任务执行逻辑（lambada表达式）
         * <p>
         * 执行步骤：
         * 1. 获取上次扫描到的最大ID（lastId）
         * 2. 查询ID大于lastId的待处理消息（status=0或3）
         * 3. 遍历消息列表，调用通知工厂发送通知
         * 4. 更新lastId为本次处理的最大ID
         * </p>
         * 
         * 注意：
         * - 如果没有待处理消息，直接返回
         * - 异常会被捕获并记录日志，不影响下次调度
         */
        Runnable task = () -> {
            try {
                // 1. 获取上次扫描到的最大ID
                long lastId = groupLastIdMap.get(groupId).get();
                
                // 2. 查询待处理的任务消息列表（ID >= lastId，status=0或3）
                List<TaskMessageEntityCommand> cmdList = localTaskMessageService.selectByHouseNumber(houseNumbers, lastId, group.getLimit());
                if (cmdList == null || cmdList.isEmpty()) {
                    return; // 没有待处理消息，直接返回
                }

                // 3. 遍历消息列表，逐个发送通知
                for (TaskMessageEntityCommand cmd : cmdList) {
                    INotifyStrategy strategy = factory.getStrategy(cmd.getNotifyType(), TaskNotifyEnum.class);
                    strategy.notify(cmd);
                }

                // 4. 更新lastId为本次处理的最大ID，用于下次增量扫描
                long maxId = cmdList.stream()
                    .map(TaskMessageEntityCommand::getId)
                    .max(Comparator.naturalOrder())
                    .orElse(lastId);
                groupLastIdMap.get(groupId).set(maxId);

                log.info("任务组 [{}] 处理完成：拉取{}条，lastId: {} -> {}", groupId, cmdList.size(), lastId, maxId);
            } catch (Exception e) {
                log.error("任务组 [{}] 执行异常: {}", groupId, e.getMessage(), e);
            }
        };

        // 根据配置选择调度方式
        if (group.getCron() != null && !group.getCron().trim().isEmpty()) {
            // 使用cron表达式调度（如："0 0/5 * * * ?" 表示每5分钟执行一次）
            scheduler.schedule(task, new CronTrigger(group.getCron()));
            log.info("任务组 [{}] 已按 cron [{}] 调度", groupId, group.getCron());
        } else {
            // 使用固定延迟调度（默认5秒）
            long delay = group.getFixedDelayMs() != null ? group.getFixedDelayMs() : 5000L;
            scheduler.scheduleWithFixedDelay(task, delay);
            log.info("任务组 [{}] 已按 fixedDelayMs [{}] 调度", groupId, delay);
        }
    }

}
