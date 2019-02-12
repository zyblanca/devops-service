package io.choerodon.devops.api.eventhandler;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.DevopsClusterE;
import io.choerodon.devops.domain.application.repository.DevopsClusterProPermissionRepository;
import io.choerodon.devops.domain.application.repository.DevopsClusterRepository;
import io.choerodon.devops.domain.application.repository.DevopsEnvironmentRepository;
import io.choerodon.devops.domain.service.DeployService;
import io.choerodon.devops.infra.common.util.EnvUtil;
import io.choerodon.websocket.helper.CommandSender;
import io.choerodon.websocket.helper.EnvListener;
import io.choerodon.websocket.session.AgentConfigurer;
import io.choerodon.websocket.session.AgentSessionManager;
import io.choerodon.websocket.session.Session;
import io.choerodon.websocket.session.SessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentInitConfig implements AgentConfigurer {

    @Autowired
    CommandSender commandSender;
    @Autowired
    DevopsEnvironmentRepository devopsEnvironmentRepository;
    @Autowired
    DevopsClusterRepository devopsClusterRepository;
    @Autowired
    DevopsClusterProPermissionRepository devopsClusterProPermissionRepository;
    @Autowired
    private DeployService deployService;
    @Autowired
    private EnvListener envListener;
    @Autowired
    private EnvUtil envUtil;
    @Value("${services.gitlab.sshUrl}")
    private String gitlabSshUrl;

    @Override
    public void registerSessionListener(AgentSessionManager agentSessionManager) {
        AgentInitListener agentInitListener = new AgentInitListener();
        agentSessionManager.addListener(agentInitListener);
    }

    class AgentInitListener implements SessionListener {
        @Override
        public void onConnected(Session session) {
            try {
                Long clusterId = Long.valueOf(session.getRegisterKey().split(":")[1]);
                List<Long> connected = envUtil.getConnectedEnvList(envListener);
                List<Long> upgraded = envUtil.getUpdatedEnvList(envListener);
                if (connected.contains(clusterId) && !upgraded.contains(clusterId)) {
                    DevopsClusterE devopsClusterE = devopsClusterRepository.query(clusterId);
                    deployService.upgradeCluster(devopsClusterE);
                }
                deployService.initCluster(clusterId);
            } catch (Exception e) {
                throw new CommonException("read envId from agent session failed", e);
            }
        }

        @Override
        public Session onClose(String s) {
            return null;
        }
    }

}
