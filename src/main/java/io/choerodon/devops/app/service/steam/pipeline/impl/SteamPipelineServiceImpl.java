package io.choerodon.devops.app.service.steam.pipeline.impl;

import com.crc.crcloud.starter.pipeline.common.PipelineConstants;
import com.crc.crcloud.starter.pipeline.common.PipelineMessage;
import com.crc.crcloud.starter.pipeline.common.PipelineMessageType;
import com.crc.crcloud.starter.pipeline.service.PipelineMsgService;
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
    PipelineMsgService pipelineMsgService;

    @Override
    public void sendGitLabWebHook(PushWebHookDTO pushWebHookDTO) {
        PipelineMessage<PushWebHookDTO> messageDTO = new PipelineMessage<>();
        messageDTO.setType(PipelineMessageType.GIT_LAB.getBizCode());
        messageDTO.setData(pushWebHookDTO);
        try {
            pipelineMsgService.sendMsg(messageDTO, PipelineConstants.PIPELINE_GIT_LAB_QUEUE);
        } catch (Exception e) {
            logger.error("发送Gitlab回调信息出现错误", e);
        }
    }
}
