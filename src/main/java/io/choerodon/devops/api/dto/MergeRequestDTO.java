package io.choerodon.devops.api.dto;

import java.util.Date;
import java.util.List;

/**
 * GitLabDTO 合并请求
 */
public class MergeRequestDTO {

    private AssigneeDTO assignee;
    private AuthorDTO author;
    private Date createdAt;
    private String description;
    private Integer id;
    private Integer iid;
    private String mergeStatus;
    private Integer projectId;
    private String sourceBranch;
    private Integer sourceProjectId;
    private String state;
    private String targetBranch;
    private Integer targetProjectId;
    private String title;
    private Date updatedAt;
    private String webUrl;

    private List<CommitDTO> commits;

    public static Boolean isValid(MergeRequestDTO mergeRequestDTO) {
        return (mergeRequestDTO != null && mergeRequestDTO.getId() != null);
    }

    public AssigneeDTO getAssignee() {
        return assignee;
    }

    public void setAssignee(AssigneeDTO assignee) {
        this.assignee = assignee;
    }

    public AuthorDTO getAuthor() {
        return author;
    }

    public void setAuthor(AuthorDTO author) {
        this.author = author;
    }

    public List<CommitDTO> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitDTO> commits) {
        this.commits = commits;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    public void setTargetProjectId(Integer targetProjectId) {
        this.targetProjectId = targetProjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

}
