package io.choerodon.devops.infra.config;

import lombok.Getter;

/**
 * @author Caiguang
 * @Description:
 * @CreateDate: 2020/1/3
 */
@Getter
public enum PipelineMessageType {
    /**
     *
     */
    CI_SCAN(1, "steam-ci-scan"),
    CI_BUILD(2, "steam-ci-build"),
    CD(3, "steam-cd"),
    GIT_LAB(4, "gitlab");

    private Integer code;
    private String bizCode;

    PipelineMessageType(Integer code, String bizCode) {
        this.code = code;
        this.bizCode = bizCode;
    }

}
