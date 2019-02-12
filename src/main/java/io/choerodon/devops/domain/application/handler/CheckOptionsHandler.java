package io.choerodon.devops.domain.application.handler;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.DevopsEnvFileResourceE;
import io.choerodon.devops.domain.application.entity.DevopsEnvironmentE;
import io.choerodon.devops.domain.application.repository.DevopsEnvFileResourceRepository;
import io.choerodon.devops.domain.application.repository.GitlabRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckOptionsHandler {

    public static final String SERVICE = "Service";
    public static final String INGRESS = "Ingress";
    public static final String CONFIGMAP = "ConfigMap";
    private static final String C7NHELM_RELEASE = "C7NHelmRelease";
    private static final String SECRET = "Secret";
    private static final String YAML_SUFFIX = ".yaml";
    private static final String RELEASE_PREFIX = "release-";
    private static final String SERVICE_PREFIX = "svc-";
    private static final String INGRESS_PREFIX = "ing-";
    private static final String SECRET_PREFIX = "sct-";
    private static final String CONFIG_PREFIX = "configMap-";

    @Autowired
    private DevopsEnvFileResourceRepository devopsEnvFileResourceRepository;

    @Autowired
    private GitlabRepository gitlabRepository;

    public void check(DevopsEnvironmentE devopsEnvironmentE, Long objectId, String code, String type) {
        String filePath = "";
        //更新实例的时候校验gitops库文件是否存在,处理部署实例时，由于没有创gitops文件导致的部署失败
        DevopsEnvFileResourceE devopsEnvFileResourceE = devopsEnvFileResourceRepository
                .queryByEnvIdAndResource(devopsEnvironmentE.getId(), objectId, type);
        switch (type) {
            case C7NHELM_RELEASE:
                filePath = RELEASE_PREFIX + code + YAML_SUFFIX;
                break;
            case INGRESS:
                filePath = INGRESS_PREFIX + code + YAML_SUFFIX;
                break;
            case SECRET:
                filePath = SECRET_PREFIX + code + YAML_SUFFIX;
                break;
            case SERVICE:
                filePath = SERVICE_PREFIX + code + YAML_SUFFIX;
                break;
            case CONFIGMAP:
                filePath = CONFIG_PREFIX + code + YAML_SUFFIX;
                break;
            default:
                break;
        }
        if (devopsEnvFileResourceE != null) {
            filePath = devopsEnvFileResourceE.getFilePath();
        }
        if (!gitlabRepository.getFile(TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()), "master",
                filePath)) {
            throw new CommonException("error.gitops.file.not.exist");
        }
    }
}
