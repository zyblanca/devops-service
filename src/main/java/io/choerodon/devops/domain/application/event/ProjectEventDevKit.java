package io.choerodon.devops.domain.application.event;


/**
 * project　event
 *
 * @author tankang
 */
public class ProjectEventDevKit extends ProjectEvent {

    /**
     * 对接DevKit,GitlabGroupId
     */
    private Long devopsAppGroupId;
    /**
     * 用户名称
     */
    private String userLogin;

    public Long getDevopsAppGroupId() {
        return devopsAppGroupId;
    }

    public void setDevopsAppGroupId(Long devopsAppGroupId) {
        this.devopsAppGroupId = devopsAppGroupId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

}
