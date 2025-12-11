package com.zsq.winter.local.message.annotation;

import java.lang.annotation.*;

/**
 * 本地任务消息注解
 * <p>
 * 该注解用于标记需要进行本地任务消息处理的方法。
 * 当方法执行时，AOP切面会拦截该方法，提取指定的任务消息命令对象，
 * 并将其保存到数据库中，同时发布事件进行异步通知。
 * </p>
 * 
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>需要可靠异步通知的业务场景（如订单支付成功后通知）</li>
 *   <li>需要保证消息不丢失的场景（先入库再异步处理）</li>
 *   <li>需要重试机制的消息发送场景</li>
 * </ul>
 * 
 * <p><b>使用示例：</b></p>
 * <pre>
 * // 示例1：entityAttributeName为空，自动从参数中查找TaskMessageEntityCommand类型
 * {@literal @}LocalTaskMessage
 * public void processOrder(TaskMessageEntityCommand command) {
 *     // 业务逻辑
 * }
 * 
 * // 示例2：指定参数名
 * {@literal @}LocalTaskMessage(entityAttributeName = "command")
 * public void processOrder(TaskMessageEntityCommand command) {
 *     // 业务逻辑
 * }
 * 
 * // 示例3：从复杂对象中提取命令
 * {@literal @}LocalTaskMessage(entityAttributeName = "request.command")
 * public void processOrder(OrderRequest request) {
 *     // 业务逻辑
 * }
 * </pre>
 * 
 * 
 * @see com.zsq.winter.local.message.config.aop.LocalTaskMessageAop
 * @see com.zsq.winter.local.message.entity.TaskMessageEntityCommand
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface LocalTaskMessage {

    /**
     * 实体属性名称，用于指定从方法参数中提取TaskMessageEntityCommand的路径
     * <p>
     * 支持以下几种用法：
     * </p>
     * <ul>
     *   <li>留空（默认）：自动从方法参数列表中查找第一个TaskMessageEntityCommand类型的对象</li>
     *   <li>"command"：直接指定参数名（参数类型必须是TaskMessageEntityCommand）</li>
     *   <li>"request.command"：对象属性路径，支持多层级访问（如request.getCommand()）</li>
     * </ul>
     * 
     * @return 实体属性名称或属性路径，默认为空字符串
     */
    String entityAttributeName() default "";

}
