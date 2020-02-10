package io.choerodon.devops.app.service.steam.integration.impl;

import com.alibaba.fastjson.JSON;
import io.choerodon.devops.api.dto.PushWebHookDTO;
import io.choerodon.devops.app.service.steam.integration.SteamPipelineService;
import io.choerodon.devops.infra.common.util.SnowflakeIdWorker;
import io.choerodon.devops.infra.config.PipelineMessage;
import io.choerodon.devops.infra.config.PipelineMessageType;
import io.choerodon.devops.infra.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * @author Caiguang
 * @Description:
 * @CreateDate: 2020/1/10
 */
@Service
@Slf4j
public class SteamPipelineServiceImpl implements SteamPipelineService {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${steam.rabbitmq.enable}")
    private boolean isEnable;

    @Override
    public void sendGitLabWebHook(PushWebHookDTO pushWebHookDTO) {
        PipelineMessage pipelineMessage = new PipelineMessage();
        pipelineMessage.setData(pushWebHookDTO);
        pipelineMessage.setType(PipelineMessageType.GIT_LAB.getBizCode());
        try {
            sendMsg(pipelineMessage);
        } catch (UnsupportedEncodingException e) {
            log.error("发送给流水线pipeline MQ 消息报错", e);
        }
    }

    public void sendMsg(PipelineMessage pipelineMessage, String queue, Long timeOut) throws UnsupportedEncodingException {
        log.info("流水线mq 发送开关是否开启:{}", isEnable);
        if (isEnable) {
            CorrelationData correlationData = new CorrelationData();
            correlationData.setId(pipelineMessage.getType() + new SnowflakeIdWorker(0, 0).nextId());
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            messageProperties.setContentEncoding("UTF-8");
            messageProperties.setExpiration(String.valueOf(timeOut));
            log.info("发送给流水线的mq类型是:{},消息内容:{}", pipelineMessage.getType(), pipelineMessage.getData());
            Message rabbitMessage = new Message(JSON.toJSONString(pipelineMessage).getBytes("utf-8"), messageProperties);
            rabbitTemplate.convertAndSend(RabbitConfig.PIPELINE_DEFAULT_EXCHANGE, queue, rabbitMessage, correlationData);
        }
    }

    /**
     * 如果没有传超时时间  默认是5分钟
     *
     * @param message
     * @param queue
     * @throws UnsupportedEncodingException
     */
    public void sendMsg(PipelineMessage message, String queue) throws UnsupportedEncodingException {
        sendMsg(message, queue, 5 * 60 * 1000L);
    }

    /**
     * 如果没传队列的话 默认传GITLAB的队列
     *
     * @param message
     * @throws UnsupportedEncodingException
     */
    public void sendMsg(PipelineMessage message) throws UnsupportedEncodingException {
        sendMsg(message, RabbitConfig.PIPELINE_GIT_LAB_QUEUE);
    }
}
