package com.zsq.winter.local.message.strategy.impl;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.zsq.winter.local.message.enums.TaskNotifyEnum;
import com.zsq.winter.local.message.service.ILocalTaskMessageService;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.strategy.INotifyStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP通知策略实现类
 * <p>
 * 实现HTTP方式的任务消息通知。
 * 通过HTTP接口调用向目标系统发送通知，支持自定义请求头和认证信息。
 * 通知成功后更新任务状态为成功，失败后更新为失败。
 * </p>
 */
@Slf4j
public class HTTPNotifyStrategy implements INotifyStrategy {

    /**
     * 任务消息仓储服务，用于更新任务状态
     */
    private final ILocalTaskMessageService repository;

    /**
     * 构造方法
     *
     * @param repository 任务消息仓储服务
     */
    public HTTPNotifyStrategy(ILocalTaskMessageService repository) {
        this.repository = repository;
    }

    /**
     * 执行HTTP通知
     * <p>
     * 调用HTTP接口向目标系统发送通知。
     * 成功后更新任务状态为2（已完成），失败后更新为3（失败）。
     * </p>
     *
     * @param command 任务消息实体命令
     * @return HTTP响应结果
     * @throws Exception HTTP请求失败时抛出异常
     */
    @Override
    public String notify(TaskMessageEntityCommand command) throws Exception {
        try {
            // 1. 获取HTTP配置
            TaskMessageEntityCommand.NotifyConfig.HTTP http = command.getNotifyConfig().getHttp();

            // 2. 构建并执行HTTP请求
            HttpRequest request = buildHttpRequest(http, command.getParameterJson());
            
            String result;
            try (HttpResponse response = request.execute()) {
                result = response.body();
            }

            // 3. 通知成功，更新状态
            repository.updateTaskStatusToSuccess(command.getTaskId());
            log.info("HTTP通知成功 - TaskId: {}, URL: {}", command.getTaskId(), http.getUrl());
            
            return result;
        } catch (Exception e) {
            // 通知失败，更新状态
            repository.updateTaskStatusToFailed(command.getTaskId());
            log.error("HTTP通知失败 - TaskId: {}, 配置: {}", 
                command.getTaskId(), JSONUtil.toJsonStr(command.getNotifyConfig()), e);
            throw e;
        }
    }

    /**
     * 构建HTTP请求
     *
     * @param http HTTP配置
     * @param bodyJson 请求体JSON
     * @return HTTP请求对象
     */
    private HttpRequest buildHttpRequest(TaskMessageEntityCommand.NotifyConfig.HTTP http, String bodyJson) {
        HttpRequest request = HttpRequest.post(http.getUrl()).body(bodyJson);

        // 设置Content-Type
        String contentType = StrUtil.isNotBlank(http.getContentType()) 
            ? http.getContentType() 
            : "application/json";
        request.header("Content-Type", contentType);

        // 设置Authorization
        if (StrUtil.isNotBlank(http.getAuthorization())) {
            request.header("Authorization", http.getAuthorization());
        }

        return request;
    }


    @Override
    public void execute(Object... params) {

    }

    @Override
    public TaskNotifyEnum getStrategyType() {
        return TaskNotifyEnum.HTTP;
    }
}
