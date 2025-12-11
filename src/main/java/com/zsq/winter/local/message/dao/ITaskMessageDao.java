package com.zsq.winter.local.message.dao;
import com.zsq.winter.local.message.entity.TaskMessagePO;
import java.sql.SQLException;
import java.util.List;

/**
 * 任务消息DAO接口
 * <p>
 * 定义任务消息的数据库访问操作，包括插入、更新和查询等功能。
 * 支持通过门牌号进行分库分表查询。
 * </p>
 * 
 * 
 */
public interface ITaskMessageDao {

    /**
     * 插入任务消息
     * <p>
     * 将任务消息存入数据库，保证消息持久化。
     * </p>
     * 
     * @param taskMessagePO 任务消息PO对象
     * @return 影响行数，成功返回1
     * @throws SQLException 数据库操作异常
     */
    int insert(TaskMessagePO taskMessagePO) throws SQLException;

    /**
     * 根据任务ID修改状态
     * <p>
     * 更新指定任务的状态，用于标记任务的处理进度。
     * </p>
     * 
     * @param taskId 任务ID
     * @param status 状态（0-待处理，1-处理中，2-已完成，3-失败）
     * @return 影响行数
     */
    int updateStatusByTaskId(String taskId, Integer status);

    /**
     * 根据门牌号查询任务消息列表
     * <p>
     * 查询指定门牌号列表中，ID大于指定值的待处理或失败状态的任务。
     * 用于定时任务扫描和重试处理。
     * </p>
     * 
     * @param houseNumbers 门牌号列表（0-9）
     * @param id 查询ID大于此值的记录
     * @param limit 限制返回结果数量
     * @return 任务消息列表，按ID升序排列
     */
    List<TaskMessagePO> selectByHouseNumber(List<Integer> houseNumbers, Long id, Integer limit);

    /**
     * 根据门牌号查询符合条件的最小ID
     * <p>
     * 查询指定门牌号列表中，处于待处理或失败状态的任务的最小ID。
     * 用于初始化任务扫描的起始ID。
     * </p>
     * 
     * @param houseNumbers 门牌号列表
     * @return 最小ID，如果没有找到则返回null
     */
    Long selectMinIdByHouseNumber(List<Integer> houseNumbers);

}
