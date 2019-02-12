package io.choerodon.devops.api.dto;

import java.util.List;

/**
 * @author zmf
 */
public class StatefulSetDTO {
    private String name;
    private Long desiredReplicas;
    private Long readyReplicas;
    private Long currentReplicas;
    private String age;
    private List<DevopsEnvPodDTO> devopsEnvPodDTOS;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDesiredReplicas() {
        return desiredReplicas;
    }

    public void setDesiredReplicas(Long desiredReplicas) {
        this.desiredReplicas = desiredReplicas;
    }

    public Long getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(Long readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public Long getCurrentReplicas() {
        return currentReplicas;
    }

    public void setCurrentReplicas(Long currentReplicas) {
        this.currentReplicas = currentReplicas;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public List<DevopsEnvPodDTO> getDevopsEnvPodDTOS() {
        return devopsEnvPodDTOS;
    }

    public void setDevopsEnvPodDTOS(List<DevopsEnvPodDTO> devopsEnvPodDTOS) {
        this.devopsEnvPodDTOS = devopsEnvPodDTOS;
    }
}
