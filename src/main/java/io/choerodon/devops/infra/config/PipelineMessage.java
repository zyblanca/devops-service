package io.choerodon.devops.infra.config;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Caiguang
 * @Description: 消息实体类
 * @CreateDate: 2019/12/30
 */
@Data
@ToString
public class PipelineMessage<T> implements Serializable {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 数据
     */
    private T data;


}
