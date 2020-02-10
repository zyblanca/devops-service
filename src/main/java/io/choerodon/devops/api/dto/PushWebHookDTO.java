package io.choerodon.devops.api.dto;

import java.util.List;

/**
 * Creator: Runge
 * Date: 2018/7/9
 * Time: 15:53
 * Description:
 */
public class PushWebHookDTO {
    private String objectKind;
    private String eventName;
    private String before;
    private String after;
    private String ref;
    private String checkoutSha;
    private Integer userId;
    private String userName;
    private String userUsername;
    private Integer projectId;
    private ProjectWebHookDto project;
    private List<CommitDTO> commits;
    private Integer totalCommitsCount;
    private String token;

    public String getObjectKind() {
        return objectKind;
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCheckoutSha() {
        return checkoutSha;
    }

    public void setCheckoutSha(String checkoutSha) {
        this.checkoutSha = checkoutSha;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public ProjectWebHookDto getProject() {
        return project;
    }

    public void setProject(ProjectWebHookDto project) {
        this.project = project;
    }

    public List<CommitDTO> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitDTO> commits) {
        this.commits = commits;
    }

    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
    }

    public void setTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    @Override
    public String toString() {
        return "PushWebHookDTO{" +
                "objectKind='" + objectKind + '\'' +
                ", eventName='" + eventName + '\'' +
                ", before='" + before + '\'' +
                ", after='" + after + '\'' +
                ", ref='" + ref + '\'' +
                ", checkoutSha='" + checkoutSha + '\'' +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", userUsername='" + userUsername + '\'' +
                ", projectId=" + projectId +
                ", project=" + project +
                ", commits=" + commits +
                ", totalCommitsCount=" + totalCommitsCount +
                ", token='" + token + '\'' +
                '}';
    }
}
