package io.choerodon.devops.api.dto;

import java.util.Date;

import io.choerodon.devops.infra.dataobject.DevopsGitlabCommitDO;

/**
 * Created by n!Ck
 * Date: 2018/9/19
 * Time: 17:18
 * Description:
 */
public class CommitFormRecordDTO {
    private Long userId;
    private Long appId;
    private String imgUrl;
    private String commitContent;
    private String userName;
    private Date commitDate;
    private String commitSHA;
    private String appName;
    private String url;

    public CommitFormRecordDTO() {
    }

    public CommitFormRecordDTO(Long userId, String imgUrl,
                               String userName,
                               DevopsGitlabCommitDO devopsGitlabCommitDO) {
        this.userId = userId;
        this.appId = devopsGitlabCommitDO.getAppId();
        this.imgUrl = imgUrl;
        this.commitContent = devopsGitlabCommitDO.getCommitContent();
        this.userName = userName;
        this.commitDate = devopsGitlabCommitDO.getCommitDate();
        this.commitSHA = devopsGitlabCommitDO.getCommitSha();
        this.appName = devopsGitlabCommitDO.getAppName();
        this.url = devopsGitlabCommitDO.getUrl();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getCommitContent() {
        return commitContent;
    }

    public void setCommitContent(String commitContent) {
        this.commitContent = commitContent;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(Date commitDate) {
        this.commitDate = commitDate;
    }

    public String getCommitSHA() {
        return commitSHA;
    }

    public void setCommitSHA(String commitSHA) {
        this.commitSHA = commitSHA;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
