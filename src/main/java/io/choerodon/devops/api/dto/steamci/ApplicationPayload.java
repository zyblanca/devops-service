package io.choerodon.devops.api.dto.steamci;

/**
 * CI应用同步实体类
 *
 * @author XIAXINYU3
 * @date 2019.8.1
 */
public class ApplicationPayload {
    private String applicationCode;
    private String applicationName;
    private Long steamProjectId;
    private Integer status;

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Long getSteamProjectId() {
        return steamProjectId;
    }

    public void setSteamProjectId(Long steamProjectId) {
        this.steamProjectId = steamProjectId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
