package io.choerodon.devops.app.service.steamci;

import io.choerodon.devops.api.dto.steamci.PrivilegePayload;

/**
 * 持续集成应用业务服务类
 *
 * @author XIAXINYU3
 * @date 2019.7.31
 */
public interface SteamCiApplicationService {
    /**
     * 处理权限
     *
     * @param payload 权限实体类
     */
    void processPrivilege(PrivilegePayload payload);
}
