package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.C7nCertificationDTO;
import io.choerodon.devops.api.dto.CertificationDTO;
import io.choerodon.devops.api.dto.OrgCertificationDTO;
import io.choerodon.devops.domain.application.valueobject.C7nCertification;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 17:43
 * Description:
 */
public interface CertificationService {

    /**
     * 创建c7n证书
     *
     * @param projectId        项目id
     * @param certificationDTO 证书
     */
    void create(Long projectId, C7nCertificationDTO certificationDTO,
                Boolean isGitOps);

    C7nCertification getC7nCertification(String name, String type, List<String> domains,
                                         String keyContent, String certContent, String envCode);

    void deleteById(Long certId);

    void certDeleteByGitOps(Long certId);

    Page<CertificationDTO> page(Long projectId, Long envId, PageRequest pageRequest, String params);

    List<CertificationDTO> getActiveByDomain(Long projectId, Long envId, String domain);

    Boolean checkCertNameUniqueInEnv(Long envId, String certName);

    Long createCertCommandE(String type, Long certId, Long userId);

    List<OrgCertificationDTO> listByProject(Long projectId);
}
