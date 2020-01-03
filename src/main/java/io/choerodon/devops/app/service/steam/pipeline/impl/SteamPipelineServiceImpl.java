package io.choerodon.devops.app.service.steam.pipeline.impl;

import com.crc.crcloud.starter.rabbitmq.common.MessageType;
import com.crc.crcloud.starter.rabbitmq.common.RabbitConstants;
import com.crc.crcloud.starter.rabbitmq.dto.MessageDTO;
import com.crc.crcloud.starter.rabbitmq.service.SendMsgService;
import io.choerodon.devops.api.dto.PushWebHookDTO;
import io.choerodon.devops.app.service.steam.pipeline.SteamPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流水线服务实现类
 *
 * @author XIAXINYU3
 * @date 2020.1.3
 */
@Service
public class SteamPipelineServiceImpl implements SteamPipelineService {
    private static final Logger logger = LoggerFactory.getLogger(SteamPipelineServiceImpl.class);

    @Autowired
    SendMsgService sendMsgService;

    @Override
    public void sendGitLabWebHook(PushWebHookDTO pushWebHookDTO) {
        MessageDTO<PushWebHookDTO> messageDTO = new MessageDTO<>();
        messageDTO.setType(MessageType.GIT_LAB.getCode());
        messageDTO.setData(pushWebHookDTO);
        try {
            sendMsgService.sendMsg(messageDTO, RabbitConstants.PIPELINE_GIT_LAB_QUEUE);
        } catch (Exception e) {
            logger.error("发送Gitlab回调信息出现错误", e);
        }
    }
}
