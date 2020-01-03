package io.choerodon.devops.app.service.steam.integration;

import io.choerodon.devops.api.dto.steamci.ApplicationPayload;
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

    /**
     * 处理应用名称
     *
     * @param payload 应用实体类
     */
    void processName(ApplicationPayload payload);

    /**
     * 处理应用转态
     *
     * @param payload 应用实体类
     */
    void processStatus(ApplicationPayload payload);
}
