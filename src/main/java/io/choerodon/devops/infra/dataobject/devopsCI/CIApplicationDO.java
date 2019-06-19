package io.choerodon.devops.infra.dataobject.devopsCI;

public class CIApplicationDO {
    private Long id;

    private String name;

    private Long groupId;

    private String gitAddress;

    private String gitSSHAddress;

    private String code;

    private Integer gitProjectId;

    private Integer status;

    private String token;

    private String dataUnit;

    private Integer unitSize;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGitAddress() {
        return gitAddress;
    }

    public void setGitAddress(String gitAddress) {
        this.gitAddress = gitAddress;
    }

    public String getGitSSHAddress() {
        return gitSSHAddress;
    }

    public void setGitSSHAddress(String gitSSHAddress) {
        this.gitSSHAddress = gitSSHAddress;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getGitProjectId() {
        return gitProjectId;
    }

    public void setGitProjectId(Integer gitProjectId) {
        this.gitProjectId = gitProjectId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDataUnit() {
        return dataUnit;
    }

    public void setDataUnit(String dataUnit) {
        this.dataUnit = dataUnit;
    }

    public Integer getUnitSize() {
        return unitSize;
    }

    public void setUnitSize(Integer unitSize) {
        this.unitSize = unitSize;
    }
}
