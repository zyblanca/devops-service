package io.choerodon.devops.app.service.steam.integration;

import io.choerodon.devops.api.dto.PushWebHookDTO;

/**
 * @author Caiguang
 * @Description:
 * @CreateDate: 2020/1/10
 */
public interface SteamPipelineService {

    /**
     * 发送代码触发mq消息给流水线
     * @param pushWebHookDTO
     */
   void sendGitLabWebHook(PushWebHookDTO pushWebHookDTO);

}
