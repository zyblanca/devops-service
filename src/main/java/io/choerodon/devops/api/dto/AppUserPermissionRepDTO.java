package io.choerodon.devops.api.dto;

/**
 * Created by n!Ck
 * Date: 2018/11/21
 * Time: 10:06
 * Description:
 */
public class AppUserPermissionRepDTO {
    private Long iamUserId;
    private String loginName;
    private String realName;

    public AppUserPermissionRepDTO() {
    }

    public AppUserPermissionRepDTO(Long iamUserId, String loginName, String realName) {
        this.iamUserId = iamUserId;
        this.loginName = loginName;
        this.realName = realName;
    }

    public Long getIamUserId() {
        return iamUserId;
    }

    public void setIamUserId(Long iamUserId) {
        this.iamUserId = iamUserId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
