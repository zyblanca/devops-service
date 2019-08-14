package io.choerodon.devops.app.service.steamci.impl;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.steamci.ApplicationPayload;
import io.choerodon.devops.api.dto.steamci.PrivilegePayload;
import io.choerodon.devops.app.service.steamci.SteamCiApplicationService;
import io.choerodon.devops.domain.application.entity.AppUserPermissionE;
import io.choerodon.devops.domain.application.entity.ApplicationE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.repository.AppUserPermissionRepository;
import io.choerodon.devops.domain.application.repository.ApplicationRepository;
import io.choerodon.devops.domain.application.repository.IamRepository;
import io.choerodon.devops.infra.dataobject.ApplicationDO;
import io.choerodon.devops.infra.mapper.ApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 持续集成应用业务服务实现类
 *
 * @author XIAXINYU3
 * @date 2019.7.31
 */
@Service
public class SteamCiApplicationServiceImpl implements SteamCiApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(SteamCiApplicationServiceImpl.class);
    private static final Integer APPLICATION_ENABLE = 1;

    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AppUserPermissionRepository appUserPermissionRepository;

    @Override
    public void processPrivilege(PrivilegePayload payload) {
        String method = payload.getMethod();
        if (RequestMethod.POST.name().equals(method)) {
            updatePrivilege(payload);
        } else if (RequestMethod.DELETE.name().equals(method)) {
            deletePrivilege(payload);
        }
    }

    private void updatePrivilege(PrivilegePayload payload) {
        logger.info("查询应用，applicationCode={}， projectId={}", payload.getApplicationCode(), payload.getSteamProjectId());
        ApplicationE applicationE = applicationRepository.queryByCode(payload.getApplicationCode(), payload.getSteamProjectId());
        if (Objects.isNull(applicationE)) {
            throw new CommonException(String.format("找不到应用, applicationCode=%s, projectId=%d", payload.getApplicationCode(), payload.getSteamProjectId()));
        }
        logger.info("查询用户，userName={}", payload.getUserName());
        UserE userE = iamRepository.queryByLoginName(payload.getUserName());
        if (Objects.isNull(userE)) {
            throw new CommonException(String.format("找不到用户, loginName=%s", payload.getUserName()));
        }
        logger.info("查询用户权限，applicationId={}", applicationE.getId());
        List<AppUserPermissionE> userPermissions = appUserPermissionRepository.listAll(applicationE.getId());
        if (CollectionUtils.isEmpty(userPermissions)) {
            logger.info("添加用户权限权限，applicationCode={}，loginName={}", payload.getApplicationCode(), payload.getUserName());
            appUserPermissionRepository.create(userE.getId(), applicationE.getId());
        } else {
            AppUserPermissionE targetUserPermission = null;
            for (AppUserPermissionE userPermission : userPermissions) {
                if (userE.getId().longValue() == userPermission.getIamUserId().longValue()) {
                    targetUserPermission = userPermission;
                }
            }
            if (Objects.isNull(targetUserPermission)) {
                logger.info("添加用户权限权限，applicationCode={}，loginName={}", payload.getApplicationCode(), payload.getUserName());
                appUserPermissionRepository.create(userE.getId(), applicationE.getId());
            } else {
                logger.info("用户权限已存在，不用再添加，applicationCode={}，loginName={}", payload.getApplicationCode(), payload.getUserName());
            }
        }
    }

    private void deletePrivilege(PrivilegePayload payload) {
        ApplicationE applicationE = applicationRepository.queryByCode(payload.getApplicationCode(), payload.getSteamProjectId());
        if (Objects.isNull(applicationE)) {
            throw new CommonException(String.format("找不到应用, applicationCode=%s, projectId=%d", payload.getApplicationCode(), payload.getSteamProjectId()));
        }
        UserE userE = iamRepository.queryByLoginName(payload.getUserName());
        if (Objects.isNull(userE)) {
            throw new CommonException(String.format("找不到用户, loginName=%s", payload.getUserName()));
        }
        logger.info("删除用户权限，applicationCode={}, projectId={}， loginName={}", payload.getApplicationCode(), payload.getSteamProjectId(), payload.getUserName());
        List<Long> appIds = new ArrayList();
        appIds.add(applicationE.getId());
        appUserPermissionRepository.deleteByUserIdWithAppIds(appIds, userE.getId());
    }

    @Override
    public void processName(ApplicationPayload payload) {
        ApplicationE applicationE = applicationRepository.queryByCode(payload.getApplicationCode(), payload.getSteamProjectId());
        if (Objects.isNull(applicationE)) {
            throw new CommonException(String.format("找不到应用, applicationCode=%s, projectId=%d", payload.getApplicationCode(), payload.getSteamProjectId()));
        }
        logger.info("更新应用名称，applicationId={}，originalApplicationName={}, updatedApplicationName={}", applicationE.getId(), applicationE.getName(), payload.getApplicationName());
        applicationMapper.updateApplicationName(applicationE.getId(), payload.getApplicationName());
    }

    @Override
    public void processStatus(ApplicationPayload payload) {
        ApplicationE applicationE = applicationRepository.queryByCode(payload.getApplicationCode(), payload.getSteamProjectId());
        if (Objects.isNull(applicationE)) {
            throw new CommonException(String.format("找不到应用, applicationCode=%s, projectId=%d", payload.getApplicationCode(), payload.getSteamProjectId()));
        }
        boolean active = (payload.getStatus().intValue() == APPLICATION_ENABLE.intValue());
        logger.info("更新应用状态，applicationId={}，oldActive={}, updatedActive={}", applicationE.getId(), applicationE.getActive(), active);
        applicationMapper.updateApplicationActive(applicationE.getId(), payload.getStatus().intValue());
    }
}
