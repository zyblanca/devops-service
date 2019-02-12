package io.choerodon.devops.api.dto;

import java.util.UUID;

/**
 * Creator: Runge
 * Date: 2018/5/8
 * Time: 16:23
 * Description:
 */
public class DevopsEnvPodContainerLogDTO {
    private String podName;
    private String containerName;
    private String logId;

    public DevopsEnvPodContainerLogDTO() {
    }

    /**
     * 覆写构造方法
     */
    public DevopsEnvPodContainerLogDTO(String podName, String containerName) {

        this.podName = podName;
        this.containerName = containerName;
        this.logId = UUID.randomUUID().toString();
    }

    public String getPodName() {

        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }
}
