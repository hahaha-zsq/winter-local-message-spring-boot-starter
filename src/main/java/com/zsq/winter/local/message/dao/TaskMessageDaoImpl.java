package com.zsq.winter.local.message.dao;

import com.zsq.winter.local.message.entity.TaskMessagePO;
import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TaskMessageDaoImpl implements ITaskMessageDao {

    private final DataSource dataSource;

    public TaskMessageDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int insert(TaskMessagePO taskMessagePO) throws SQLException {
        String sql = "INSERT INTO local_task_message (task_id, task_name, notify_type, notify_config, status, parameter_json, house_number ,create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, taskMessagePO.getTaskId());
            ps.setString(2, taskMessagePO.getTaskName());
            ps.setString(3, taskMessagePO.getNotifyType());
            ps.setString(4, taskMessagePO.getNotifyConfig());
            ps.setInt(5, taskMessagePO.getStatus());
            ps.setString(6, taskMessagePO.getParameterJson());
            ps.setInt(7, taskMessagePO.getHouseNumber());
            ps.setObject(8, taskMessagePO.getCreateTime());
            ps.setObject(9, taskMessagePO.getUpdateTime());

            return ps.executeUpdate();

        } catch (SQLException e) {
            log.error("插入任务消息失败，taskId: {}", taskMessagePO.getTaskId(), e);
            throw e;
        }
    }

    @Override
    public int updateStatusByTaskId(String taskId, Integer status) {
        String sql = "UPDATE local_task_message SET status = ?, update_time = NOW() WHERE task_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, status);
            ps.setString(2, taskId);

            return ps.executeUpdate();

        } catch (SQLException e) {
            log.error("更新任务消息状态失败，taskId: {}, status: {}", taskId, status, e);
            throw new RuntimeException("TASK_MESSAGE_UPDATE_STATUS_ERROR 更新任务消息状态失败 " + e.getMessage());
        }
    }
    /**
     * 实现了一个带有 IN 列表过滤、id 游标分页、status 过滤、limit 限制、按 id 升序排序的数据库查询，并将每条记录转换成 Java 对象返回
     * <pre>{@code
     *     SELECT id, task_id, task_name, notify_type, notify_config, status, parameter_json,
     *        house_number, create_time, update_time
     * FROM local_task_message
     * WHERE id >= ?
     *   AND house_number IN (?, ?, ?, ...)
     *   AND status IN (0, 3)
     * ORDER BY id ASC
     * LIMIT ?
     *}</pre>
     *
     * @param houseNumbers 门牌号列表
     * @param id           id
     * @param limit        限制数量
     * @return 任务消息列表
     */
    @Override
    public List<TaskMessagePO> selectByHouseNumber(List<Integer> houseNumbers, Long id, Integer limit) {
        if (houseNumbers == null || houseNumbers.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建 IN 子句的占位符
        String sql = getString(houseNumbers);

        List<TaskMessagePO> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            // 设置参数
            int paramIndex = 1;
            ps.setLong(paramIndex++, id);

            // 设置门牌号参数
            for (Integer houseNumber : houseNumbers) {
                ps.setInt(paramIndex++, houseNumber);
            }

            ps.setInt(paramIndex, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskMessagePO taskMessagePO = new TaskMessagePO();
                    taskMessagePO.setId(rs.getLong("id"));
                    taskMessagePO.setTaskId(rs.getString("task_id"));
                    taskMessagePO.setTaskName(rs.getString("task_name"));
                    taskMessagePO.setNotifyType(rs.getString("notify_type"));
                    taskMessagePO.setNotifyConfig(rs.getString("notify_config"));
                    taskMessagePO.setStatus(rs.getInt("status"));
                    taskMessagePO.setParameterJson(rs.getString("parameter_json"));
                    taskMessagePO.setHouseNumber(rs.getInt("house_number"));

                    // 处理时间字段
                    if (rs.getTimestamp("create_time") != null) {
                        taskMessagePO.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    }
                    if (rs.getTimestamp("update_time") != null) {
                        taskMessagePO.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                    }

                    result.add(taskMessagePO);
                }
            }

            return result;

        } catch (SQLException e) {
            log.error("根据门牌号列表查询任务消息失败，houseNumbers: {}, id: {}, limit: {}", houseNumbers, id, limit, e);
            throw new RuntimeException("TASK_MESSAGE_SELECT_BY_HOUSE_NUMBER_ERROR 根据门牌号列表查询任务消息失败 " + e.getMessage());
        }
    }

    private static String getString(List<Integer> houseNumbers) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < houseNumbers.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }

        return "SELECT id, task_id, task_name, notify_type, notify_config, status, parameter_json, house_number, create_time, update_time " +
                "FROM local_task_message WHERE id >= ? AND house_number IN (" + placeholders + ") AND status IN (0, 3) ORDER BY id ASC LIMIT ?";
    }

    /**
     * 根据多个门牌号查询状态为 0 或 3 的记录，并返回最小 id 的安全 SQL 查询代码
     *
     * @param houseNumbers 门牌号列表
     * @return 最小的 id
     */
    @Override
    public Long selectMinIdByHouseNumber(List<Integer> houseNumbers) {
        if (houseNumbers == null || houseNumbers.isEmpty()) {
            return null;
        }

        // 构建 IN 子句的占位符
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < houseNumbers.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }

        String sql = "SELECT MIN(id) as min_id FROM local_task_message WHERE house_number IN (" + placeholders + ") AND status IN (0, 3)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            // 设置门牌号参数
            for (int i = 0; i < houseNumbers.size(); i++) {
                ps.setInt(i + 1, houseNumbers.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("min_id");
                }
                return null;
            }

        } catch (SQLException e) {
            log.error("根据门牌号列表查询最小ID失败，houseNumbers: {}", houseNumbers, e);
            throw new RuntimeException("根据门牌号列表查询最小ID失败，houseNumbers: " + houseNumbers + e.getMessage());
        }
    }

}