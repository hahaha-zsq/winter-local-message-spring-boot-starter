package com.zsq.winter.local.message.service;


import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;

import java.util.List;

/**
 * 本地任务消息仓储接口
 * <p>
 * 定义任务消息的仓储层接口，提供任务消息的增、改、查等基本操作。
 * 作为领域层与数据层的桥梁，封装数据库操作逻辑。
 * </p>
 *
 * 
 * @since 2025/11/12 07:50
 */
public interface ILocalTaskMessageService {

    /**
     * 保存任务消息
     * <p>
     * 将任务消息存入数据库，并自动计算门牌号进行分表。
     * </p>
     * 
     * @param command 任务消息实体命令
     * @throws Exception 保存失败时抛出异常
     */
    void saveTaskMessage(TaskMessageEntityCommand command) throws Exception;

    /**
     * 更新任务状态为成功
     * <p>
     * 将指定任务的状态更新为2（已完成）。
     * </p>
     * 
     * @param taskId 任务ID
     */
    void updateTaskStatusToSuccess(String taskId);

    /**
     * 更新任务状态为失败
     * <p>
     * 将指定任务的状态更新为3（失败），支持重试机制。
     * </p>
     * 
     * @param taskId 任务ID
     */
    void updateTaskStatusToFailed(String taskId);

    /**
     * 根据门牌号查询任务消息列表
     * <p>
     * 查询指定门牌号列表中的待处理或失败的任务。
     * 用于定时任务扫描和重试。
     * </p>
     * 
     * @param houseNumbers 门牌号列表
     * @param id 查询ID大于此值的记录
     * @param limit 限制返回结果数量
     * @return 任务消息列表
     */
    List<TaskMessageEntityCommand> selectByHouseNumber(List<Integer> houseNumbers, Long id, Integer limit);

    /**
     * 根据门牌号查询符合条件的最小ID
     * <p>
     * 用于初始化定时任务的扫描起始ID。
     * </p>
     * 
     * @param houseNumbers 门牌号列表
     * @return 最小ID，如果没有找到则返回null
     */
    Long selectMinIdByHouseNumber(List<Integer> houseNumbers);

}
