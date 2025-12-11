package com.zsq.winter.local.message.config.aop;


import com.zsq.winter.local.message.annotation.LocalTaskMessage;
import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import com.zsq.winter.local.message.LocalTaskMessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 本地任务消息AOP切面
 * <p>
 * 该切面用于拦截标注了@LocalTaskMessage注解的方法，自动提取任务消息命令并进行处理。
 * 主要功能包括：
 * 1. 事务管理：支持与现有事务集成或开启新事务，确保业务操作与消息保存的原子性
 * 2. 消息提取：从方法参数中智能提取TaskMessageEntityCommand对象
 * 3. 消息处理：调用LocalTaskMessageTemplate保存消息并发布相关事件
 * </p>
 *
 * <p><b>切面执行流程：</b></p>
 * <ol>
 *   <li>检查当前是否存在活跃事务</li>
 *   <li>执行目标业务方法</li>
 *   <li>根据注解配置从方法参数中提取TaskMessageEntityCommand</li>
 *   <li>调用handleService保存任务消息到数据库并发布Spring事件</li>
 *   <li>如果发生异常，根据事务状态进行回滚处理</li>
 * </ol>
 *
 * <p><b>事务处理策略：</b></p>
 * <ul>
 *   <li>如果当前已存在事务：直接在现有事务中执行，保证与业务操作的一致性</li>
 *   <li>如果当前无事务：开启新的事务，将业务方法和消息处理包装在同一事务中</li>
 * </ul>
 * 
 * @author dadandiaoming
 * @see LocalTaskMessage 本地任务消息注解
 * @see LocalTaskMessageTemplate 本地任务消息处理模板
 * @see TaskMessageEntityCommand 任务消息实体命令
 */
@Slf4j
@Aspect
public class LocalTaskMessageAop {
    
    /** 本地任务消息处理模板，用于保存消息和发布事件 */
    private final LocalTaskMessageTemplate handleService;
    
    /** Spring事务模板，用于在无事务环境下开启新事务 */
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造函数
     * 
     * @param handleService 本地任务消息处理服务
     * @param transactionTemplate Spring事务模板
     */
    public LocalTaskMessageAop(LocalTaskMessageTemplate handleService,
                               TransactionTemplate transactionTemplate) {
        this.handleService = handleService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 定义切点：拦截所有标注了@LocalTaskMessage注解的方法
     * 
     * 该切点会匹配任何使用@LocalTaskMessage注解标记的方法，
     * 无论方法在哪个类中，只要有该注解就会被AOP拦截处理
     */
    @Pointcut("@annotation(com.zsq.winter.local.message.annotation.LocalTaskMessage)")
    public void aopPoint() {
    }

    /**
     * 环绕通知：处理本地任务消息
     * 
     * 该方法是AOP的核心处理逻辑，会在目标方法执行前后进行拦截处理。
     * 主要职责：
     * 1. 执行目标业务方法
     * 2. 从方法参数中提取任务消息命令对象
     * 3. 保存任务消息到数据库
     * 4. 发布Spring事件通知其他组件
     * 
     * @param joinPoint 连接点，包含目标方法的信息和参数
     * @param localTaskMessage 本地任务消息注解实例，包含配置信息
     * @return 目标方法的返回值
     * @throws Throwable 目标方法或消息处理过程中的异常
     */
    @Around("aopPoint() && @annotation(localTaskMessage)")
    public Object notify(ProceedingJoinPoint joinPoint, LocalTaskMessage localTaskMessage) throws Throwable {
        // 获取方法签名，用于日志记录
        String signature = joinPoint.getSignature().toShortString();
        // 从注解中获取实体属性名称，用于定位TaskMessageEntityCommand对象
        String entityAttributeName = localTaskMessage.entityAttributeName();

        // 检查当前线程是否已经存在活跃的事务
        // 如果存在事务则复用，如果不存在则需要开启新事务来保证数据一致性
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();

        // 情况1：当前已存在活跃事务，直接在现有事务中执行
        if (active) {
            try {
                // 先执行目标业务方法
                Object result = joinPoint.proceed();
                
                // 从方法参数中解析出TaskMessageEntityCommand对象
                TaskMessageEntityCommand command = resolveCommand(joinPoint, entityAttributeName);
                
                if (command != null) {
                    log.info("LocalTaskMessageAop 提取到命令对象(同一事务): 方法={} 路径={} 命令={}", signature, entityAttributeName, command);
                    // 在同一事务中保存任务消息，如果业务方法回滚，消息也会一起回滚
                    handleService.acceptTaskMessage(command);
                } else {
                    log.warn("LocalTaskMessageAop 未能提取命令对象(同一事务): 方法={} 路径={}", signature, entityAttributeName);
                }
                return result;
            } catch (Throwable e) {
                log.error("LocalTaskMessageAop 处理失败: 方法={} 路径={} 错误={}", signature, entityAttributeName, e.getMessage(), e);
                // 重新抛出异常，让事务管理器处理回滚
                throw e;
            }
        }

        // 情况2：当前无事务，开启新事务并将业务方法执行与消息处理统一包装在事务中
        try {
            return transactionTemplate.execute(status -> {
                try {
                    // 在新事务中先执行目标业务方法
                    Object result = joinPoint.proceed();
                    
                    // 从方法参数中解析出TaskMessageEntityCommand对象
                    TaskMessageEntityCommand command = resolveCommand(joinPoint, entityAttributeName);
                    
                    if (command != null) {
                        log.info("LocalTaskMessageAop 提取到命令对象(新开事务): 方法={} 路径={} 命令={}", signature, entityAttributeName, command);
                        // 在同一新事务中保存任务消息，保证业务操作与消息保存的原子性
                        handleService.acceptTaskMessage(command);
                    } else {
                        log.warn("LocalTaskMessageAop 未能提取命令对象(新开事务): 方法={} 路径={}", signature, entityAttributeName);
                    }
                    return result;
                } catch (Throwable t) {
                    log.error("LocalTaskMessageAop 事务内处理失败: 方法={} 路径={} 错误={}", signature, entityAttributeName, t.getMessage(), t);
                    // 标记事务为只回滚状态，确保数据一致性
                    status.setRollbackOnly();
                    // 将检查异常包装为运行时异常，以便事务管理器能够正确处理
                    throw new RuntimeException(t);
                }
            });
        } catch (RuntimeException e) {
            // 如果是包装的异常，则解包并重新抛出原始异常
            if (e.getCause() != null) throw e.getCause();
            throw e;
        }
    }

    /**
     * 从方法参数中解析TaskMessageEntityCommand对象
     * 
     * 该方法支持多种灵活的参数解析方式：
     * 
     * <p><b>支持的解析模式：</b></p>
     * <ol>
     *   <li><b>直接参数模式：</b> entityAttributeName = "command"
     *       <br>直接从方法参数中查找名为"command"且类型为TaskMessageEntityCommand的参数</li>
     *   
     *   <li><b>对象属性路径模式：</b> entityAttributeName = "request.command" 
     *       <br>先找到名为"request"的参数，然后调用其getCommand()方法或访问command字段</li>
     *   
     *   <li><b>自动发现模式：</b> entityAttributeName为空或null
     *       <br>自动从所有参数中查找第一个TaskMessageEntityCommand类型的对象</li>
     * </ol>
     * 
     * <p><b>属性访问策略：</b></p>
     * <ul>
     *   <li>优先尝试调用getter方法（如getCommand()）</li>
     *   <li>如果getter方法不存在，则尝试直接访问字段</li>
     *   <li>支持多级属性路径，如"request.data.command"</li>
     * </ul>
     * 
     * @param joinPoint 连接点，包含方法信息和参数数组
     * @param entityAttributeName 实体属性名称路径，支持点分隔的多级路径
     * @return 解析出的TaskMessageEntityCommand对象，如果解析失败则返回null
     */
    private TaskMessageEntityCommand resolveCommand(ProceedingJoinPoint joinPoint, String entityAttributeName) {
        // 获取方法的所有参数
        Object[] args = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        // 模式1：自动发现模式 - 如果未配置属性路径，直接从参数列表中查找TaskMessageEntityCommand类型
        if (entityAttributeName == null || entityAttributeName.trim().isEmpty()) {
            for (Object arg : args) {
                if (arg instanceof TaskMessageEntityCommand) {
                    return (TaskMessageEntityCommand) arg;
                }
            }
            return null;
        }

        // 解析属性路径，支持多级路径如"request.data.command"
        String[] path = entityAttributeName.split("\\.");
        String paramName = path[0]; // 第一段是方法参数名

        // 根据参数名或类型在方法参数中查找根对象
        Object root = findArgumentByNameOrType(method, args, paramName);
        if (root == null) {
            return null;
        }

        // 模式2：直接参数模式 - 如果路径只有一段且根对象就是TaskMessageEntityCommand类型
        if (path.length == 1) {
            return (root instanceof TaskMessageEntityCommand) ? (TaskMessageEntityCommand) root : null;
        }

        // 模式3：对象属性路径模式 - 沿着属性路径逐级访问对象属性
        Object current = root;
        for (int i = 1; i < path.length; i++) {
            if (current == null) return null;
            // 从当前对象中提取下一级属性
            current = extractProperty(current, path[i]);
        }

        // 检查最终对象是否为TaskMessageEntityCommand类型
        return (current instanceof TaskMessageEntityCommand) ? (TaskMessageEntityCommand) current : null;
    }

    /**
     * 根据参数名或类型在方法参数中查找对应的对象
     * 
     * 该方法采用两阶段查找策略：
     * 1. 首先尝试通过反射获取的参数名进行精确匹配
     * 2. 如果参数名匹配失败，则根据特定规则进行类型匹配
     * 
     * <p><b>注意：</b></p>
     * <ul>
     *   <li>参数名匹配需要编译时保留参数名信息（-parameters编译选项或调试信息）</li>
     *   <li>类型匹配仅在参数名为"command"时启用，避免误匹配其他TaskMessageEntityCommand类型参数</li>
     * </ul>
     * 
     * @param method 目标方法对象
     * @param args 方法参数数组
     * @param paramName 要查找的参数名称
     * @return 匹配的参数对象，如果未找到则返回null
     */
    private Object findArgumentByNameOrType(Method method, Object[] args, String paramName) {
        // 策略1：通过反射获取参数名进行精确匹配
        // 注意：需要编译时使用-parameters选项或包含调试信息才能获取到真实的参数名
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        if (parameters.length == args.length) {
            for (int i = 0; i < parameters.length; i++) {
                if (Objects.equals(parameters[i].getName(), paramName)) {
                    return args[i];
                }
            }
        }

        // 策略2：类型匹配回退机制
        // 仅在参数名为"command"时启用类型匹配，避免误匹配其他TaskMessageEntityCommand类型的参数
        // 这是一个安全的回退策略，因为"command"是一个常见的TaskMessageEntityCommand参数名
        if ("command".equals(paramName)) {
            for (Object arg : args) {
                if (arg instanceof TaskMessageEntityCommand) {
                    return arg;
                }
            }
        }

        return null;
    }

    /**
     * 从对象中按属性名提取属性值
     * 
     * 该方法采用两阶段属性访问策略：
     * 1. 优先尝试调用标准的getter方法（如getCommand()）
     * 2. 如果getter方法不存在或调用失败，则尝试直接访问字段
     * 
     * <p><b>访问规则：</b></p>
     * <ul>
     *   <li>getter方法名规则：get + 首字母大写的属性名</li>
     *   <li>字段访问会自动设置accessible=true以访问私有字段</li>
     *   <li>如果两种方式都失败，返回null而不抛出异常</li>
     * </ul>
     * 
     * @param target 目标对象
     * @param propertyName 属性名称
     * @return 属性值，如果获取失败则返回null
     */
    private Object extractProperty(Object target, String propertyName) {
        Class<?> clazz = target.getClass();
        
        // 策略1：尝试调用getter方法
        // 构造getter方法名：get + 首字母大写的属性名
        String getterName = "get" + capitalize(propertyName);
        try {
            Method getter = clazz.getMethod(getterName);
            return getter.invoke(target);
        } catch (Exception ignore) {
            // 忽略异常，继续尝试字段访问
        }

        // 策略2：直接访问字段
        // 当getter方法不存在或调用失败时，尝试直接访问同名字段
        try {
            Field field = clazz.getDeclaredField(propertyName);
            // 设置字段可访问，以便访问私有字段
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            // 如果字段访问也失败，返回null
            return null;
        }
    }

    /**
     * 将字符串首字母大写
     * 
     * 用于构造getter方法名，例如：
     * - "command" -> "Command"
     * - "taskId" -> "TaskId"
     * - "Command" -> "Command" (已经大写的保持不变)
     * 
     * @param s 输入字符串
     * @return 首字母大写的字符串，如果输入为null或空字符串则原样返回
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        char first = s.charAt(0);
        // 如果首字母已经是大写，直接返回原字符串
        if (Character.isUpperCase(first)) return s;
        // 将首字母转为大写，其余部分保持不变
        return Character.toUpperCase(first) + s.substring(1);
    }

}
