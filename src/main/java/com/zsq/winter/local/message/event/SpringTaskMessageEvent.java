package com.zsq.winter.local.message.event;

import com.zsq.winter.local.message.entity.TaskMessageEntityCommand;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring任务消息事件
 * <p>
 * Spring中，事件源不强迫继承ApplicationEvent接口的，也就是可以直接发布任意一个对象类(实体类，Map，List，String等任意对象类）。但内部其实是使用PayloadApplicationEvent类进行包装了一层。
 * 继承Spring的ApplicationEvent，用于封装任务消息命令并在Spring容器中传播。
 * 该事件由LocalTaskMessageEvent发布，由TaskMessageEventListener监听处理。
 * </p>
 * 
 * 
 * @see org.springframework.context.ApplicationEvent
 */
@Getter
public class SpringTaskMessageEvent extends ApplicationEvent {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = -5580485467582771923L;

    /**
     * 任务消息实体命令
     */
    private final TaskMessageEntityCommand taskMessageEntityCommand;

    /**
     * 构造方法
     * 
     * @param source 事件源
     * @param taskMessageEntityCommand 任务消息实体命令
     */
    public SpringTaskMessageEvent(Object source, TaskMessageEntityCommand taskMessageEntityCommand) {
        super(source);
        this.taskMessageEntityCommand = taskMessageEntityCommand;
    }

    @Override
    public String toString() {
        return "TaskMessageEvent{" +
                "taskMessage='" + taskMessageEntityCommand + '\'' +
                '}';
    }

}
