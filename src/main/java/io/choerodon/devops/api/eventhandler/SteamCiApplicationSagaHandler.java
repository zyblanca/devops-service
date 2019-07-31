package io.choerodon.devops.api.eventhandler;

import com.google.gson.Gson;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.devops.api.dto.steamci.PrivilegePayload;
import io.choerodon.devops.app.service.steamci.SteamCiApplicationService;
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
    @Autowired
    private SteamCiApplicationService steamCiApplicationService;

    @SagaTask(code = "steamCiApplicationPrivilegeSync",
            description = "CI应用权限同步服务",
            sagaCode = SteamCiApplicationSagaHandler.APPLICATION_PRIVILEGE_SAGA_CODE,
            maxRetryCount = 1,
            seq = 1)
    public void createApplication(String data) {
        logger.info("CI应用权限同步服务请求参数：{}", data);
        Gson gson = new Gson();
        PrivilegePayload privilegePayload = gson.fromJson(data, PrivilegePayload.class);
        steamCiApplicationService.processPrivilege(privilegePayload);
        logger.info("完成CI应用权限同步服务");
    }
}
