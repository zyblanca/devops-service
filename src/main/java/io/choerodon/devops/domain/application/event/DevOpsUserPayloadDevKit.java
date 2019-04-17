package io.choerodon.devops.domain.application.event;

/**
 * Created tankang
 */
public class DevOpsUserPayloadDevKit extends DevOpsUserPayload {

    // 对接DevKit,应用名称,Git地址,用户名称
    private String itemName;
    private String gitAddress;

    private Boolean onlyModifyApplication;

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public String getGitAddress() {
        return gitAddress;
    }

    public void setGitAddress(String gitAddress) {
        this.gitAddress = gitAddress;
    }

    public Boolean getOnlyModifyApplication() {
        return onlyModifyApplication;
    }

    public void setOnlyModifyApplication(Boolean onlyModifyApplication) {
        this.onlyModifyApplication = onlyModifyApplication;
    }
}

