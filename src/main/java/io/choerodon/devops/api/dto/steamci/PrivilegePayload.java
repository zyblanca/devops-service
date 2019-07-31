package io.choerodon.devops.api.dto.steamci;

/**
 * CI应用权限同步实体类
 *
 * @author XIAXINYU3
 * @date 2019.7.31
 */
public class PrivilegePayload {
    private String method;
    private Long applicationId;
    private String applicationCode;
    private String applicationName;
    private Long steamProjectId;
    private Integer gitProjectId;
    private Integer user;
    private String userName;
    private String accessLevel;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Integer getGitProjectId() {
        return gitProjectId;
    }

    public void setGitProjectId(Integer gitProjectId) {
        this.gitProjectId = gitProjectId;
    }

    public Integer getUser() {
        return user;
    }

    public void setUser(Integer user) {
        this.user = user;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public Long getSteamProjectId() {
        return steamProjectId;
    }

    public void setSteamProjectId(Long steamProjectId) {
        this.steamProjectId = steamProjectId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
