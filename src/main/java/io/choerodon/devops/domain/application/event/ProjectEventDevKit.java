package io.choerodon.devops.domain.application.event;

import java.util.List;

/**
 * project　event
 *
 * @author tankang
 */
public class ProjectEventDevKit {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String organizationCode;
    private String organizationName;
    private String userName;
    private Long userId;
    private List<String> roleLabels;

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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getRoleLabels() {
        return roleLabels;
    }

    public void setRoleLabels(List<String> roleLabels) {
        this.roleLabels = roleLabels;
    }
}
