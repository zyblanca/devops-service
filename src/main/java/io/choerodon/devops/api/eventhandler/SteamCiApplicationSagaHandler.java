package io.choerodon.devops.api.eventhandler;

import com.google.gson.Gson;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.devops.api.dto.steamci.ApplicationPayload;
import io.choerodon.devops.api.dto.steamci.PrivilegePayload;
import io.choerodon.devops.app.service.steam.integration.SteamCiApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 持续集成应用业务服务saga任务处理
 *
 * @author XIAXINYU3
 * @date 2019.7.31
 */
@Component
public class SteamCiApplicationSagaHandler {
    private static final Logger logger = LoggerFactory.getLogger(SteamCiApplicationSagaHandler.class);
    private static final String APPLICATION_PRIVILEGE_SAGA_CODE = "steam-ci-application-privilege-sync-service";
    private static final String APPLICATION_STATUS_SAGA_CODE = "steam-ci-application-status-sync-service";
    private static final String APPLICATION_NAME_SAGA_CODE = "steam-ci-application-name-sync-service";

    @Autowired
    private SteamCiApplicationService steamCiApplicationService;

    @SagaTask(code = "steamCiApplicationPrivilegeSync",
            description = "CI应用权限同步服务",
            sagaCode = SteamCiApplicationSagaHandler.APPLICATION_PRIVILEGE_SAGA_CODE,
            maxRetryCount = 1,
            seq = 1)
    public void processPrivilege(String data) {
        logger.info("CI应用权限同步服务请求参数：{}", data);
        Gson gson = new Gson();
        PrivilegePayload privilegePayload = gson.fromJson(data, PrivilegePayload.class);
        steamCiApplicationService.processPrivilege(privilegePayload);
        logger.info("完成CI应用权限同步服务");
    }

    @SagaTask(code = "devopsServiceApplicationStatusSync",
            description = "CI应用状态同步服务",
            sagaCode = SteamCiApplicationSagaHandler.APPLICATION_STATUS_SAGA_CODE,
            maxRetryCount = 1,
            seq = 1)
    public String processStatus(String data) {
        logger.info("CI应用状态同步服务请求参数：{}", data);
        Gson gson = new Gson();
        ApplicationPayload payload = gson.fromJson(data, ApplicationPayload.class);
        steamCiApplicationService.processStatus(payload);
        logger.info("完成CI应用状态同步服务");
        return data;
    }

    @SagaTask(code = "devopsServiceApplicationNameSync",
            description = "CI应用名称同步服务",
            sagaCode = SteamCiApplicationSagaHandler.APPLICATION_NAME_SAGA_CODE,
            maxRetryCount = 1,
            seq = 1)
    public String processName(String data) {
        logger.info("CI应用名称同步服务请求参数：{}", data);
        Gson gson = new Gson();
        ApplicationPayload payload = gson.fromJson(data, ApplicationPayload.class);
        steamCiApplicationService.processName(payload);
        logger.info("完成CI应用名称同步服务");
        return data;
    }
}
