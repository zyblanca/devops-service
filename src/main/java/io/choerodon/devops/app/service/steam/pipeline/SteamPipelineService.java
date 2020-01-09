package io.choerodon.devops.app.service.steam.pipeline;

import io.choerodon.devops.api.dto.PushWebHookDTO;

/**
 * 流水线服务实现类
 *
 * @author XIAXINYU3
 * @date 2020.1.3
 */
public interface SteamPipelineService {

    /**
     * 发送Gitlab回调信息
     *
     * @param pushWebHookDTO 代码更新数据回调实体
     */
    void sendGitLabWebHook(PushWebHookDTO pushWebHookDTO);
}
