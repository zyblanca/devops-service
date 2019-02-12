package io.choerodon.devops.domain.application.entity;

public class DevopsEnvGroupE {

    private Long id;
    private ProjectE projectE;
    private String name;
    private Long sequence;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectE getProjectE() {
        return projectE;
    }

    public void initProject(Long projectId) {
        this.projectE = new ProjectE(projectId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectE(ProjectE projectE) {
        this.projectE = projectE;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
}
