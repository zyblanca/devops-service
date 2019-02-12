package io.choerodon.devops.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creator: Runge
 * Date: 2018/4/20
 * Time: 14:56
 * Description:
 */
public class DevopsIngressDTO {

    private Long id;
    private String domain;
    private String name;
    private Long envId;
    private String envName;
    private Boolean envStatus;
    private Boolean isUsable;
    private String status;
    private Long certId;
    private String certName;
    private String certStatus;
    private List<DevopsIngressPathDTO> pathList;
    private String commandType;
    private String commandStatus;
    private String error;

    public DevopsIngressDTO() {
    }

    /**
     * 构造函数
     */
    public DevopsIngressDTO(Long id, String domain, String name,
                            Long envId, Boolean isUsable, String envName) {
        this.envId = envId;
        this.id = id;
        this.name = name;
        this.domain = domain;
        this.pathList = new ArrayList<>();
        this.isUsable = isUsable;
        this.envName = envName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getEnvStatus() {
        return envStatus;
    }

    public void setEnvStatus(Boolean envStatus) {
        this.envStatus = envStatus;
    }

    public List<DevopsIngressPathDTO> getPathList() {
        return pathList;
    }

    public void setPathList(List<DevopsIngressPathDTO> pathList) {
        this.pathList = pathList;
    }

    public DevopsIngressPathDTO queryLastDevopsIngressPathDTO() {
        Integer size = pathList.size();
        return size == 0 ? null : pathList.get(size - 1);
    }

    public void addDevopsIngressPathDTO(DevopsIngressPathDTO devopsIngressPathDTO) {
        this.pathList.add(devopsIngressPathDTO);
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public Boolean getUsable() {
        return isUsable;
    }

    public void setUsable(Boolean usable) {
        isUsable = usable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCertId() {
        return certId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public String getCertName() {
        return certName;
    }

    public void setCertName(String certName) {
        this.certName = certName;
    }

    public String getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(String certStatus) {
        this.certStatus = certStatus;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus = commandStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DevopsIngressDTO that = (DevopsIngressDTO) o;
        return Objects.equals(domain, that.domain)
                && Objects.equals(name, that.name)
                && Objects.equals(envId, that.envId)
                && Objects.equals(pathList, that.pathList)
                && Objects.equals(certId, that.certId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, name, envId, pathList);
    }
}
