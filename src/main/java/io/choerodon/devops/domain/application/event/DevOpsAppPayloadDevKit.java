package io.choerodon.devops.domain.application.event;

/**
 * @author tankang
 * 对接DevKit,,,
 */
public class DevOpsAppPayloadDevKit extends DevOpsAppPayload {

    /**
     * 应用名称
     */
    private String itemName;
    /**
     * Git地址
     */
    private String gitAddress;
    /**
     * 用户名称
     */
    private String userLogin;

    /**
     * 组织编码
     */
    private String organizationCode;

    /**
     * GitLabToken
     */
    private String token;

    /**
     * 状态,1:启用 0:禁用
     */
    private Boolean status;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getGitAddress() {
        return gitAddress;
    }

    public void setGitAddress(String gitAddress) {
        this.gitAddress = gitAddress;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }
}
