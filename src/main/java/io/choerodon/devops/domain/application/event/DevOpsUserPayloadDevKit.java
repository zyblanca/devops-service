package io.choerodon.devops.domain.application.event;

/**
 * Created tankang
 */
public class DevOpsUserPayloadDevKit extends DevOpsUserPayload {

    // 对接DevKit,应用名称,Git地址,用户名称
    private String itemName;
    private String gitAddress;
    private String userLogin;

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
}

