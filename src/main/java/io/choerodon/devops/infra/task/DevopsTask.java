package io.choerodon.devops.infra.task;

import io.choerodon.asgard.schedule.QuartzDefinition;
import io.choerodon.asgard.schedule.annotation.JobParam;
import io.choerodon.asgard.schedule.annotation.JobTask;
import io.choerodon.asgard.schedule.annotation.TaskParam;
import io.choerodon.asgard.schedule.annotation.TimedTask;
import io.choerodon.devops.app.service.DevopsCheckLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author zmf
 */
@Component
public class DevopsTask {
    private static final Logger logger = LoggerFactory.getLogger(DevopsTask.class);

    @Autowired
    private DevopsCheckLogService devopsCheckLogService;

    @JobTask(maxRetryCount = 3, code = "syncDevopsEnvPodFields", params = {
            @JobParam(name = "test", defaultValue = "test")
    }, description = "升级到0.14.0处理Pod表的节点名称和重启次数字段")
    @TimedTask(name = "syncDevopsEnvPodFields", description = "升级到0.14.0处理Pod表的节点名称和重启次数字段", oneExecution = true,
            repeatCount = 0, repeatInterval = 1, repeatIntervalUnit = QuartzDefinition.SimpleRepeatIntervalUnit.HOURS, params = {
            @TaskParam(name = "test", value = "test")
    })
    public void syncDevopsEnvPodFields(Map<String, Object> map) {
        logger.info("begin to sync pod node name and restart count");
        devopsCheckLogService.checkLog("0.14.0");
    }
}
