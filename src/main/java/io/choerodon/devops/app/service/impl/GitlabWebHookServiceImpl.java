package io.choerodon.devops.app.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.app.service.steam.integration.SteamPipelineService;
import io.choerodon.devops.infra.common.util.FastjsonParserConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.domain.application.entity.DevopsMergeRequestE;
import io.choerodon.devops.domain.application.repository.DevopsMergeRequestRepository;

@Service
public class GitlabWebHookServiceImpl implements GitlabWebHookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabWebHookServiceImpl.class);

    private DevopsMergeRequestRepository devopsMergeRequestRepository;
    private DevopsGitService devopsGitService;
    private DevopsGitlabCommitService devopsGitlabCommitService;
    private DevopsGitlabPipelineService devopsGitlabPipelineService;
    private DevOpsCIService devOpsCIService;
    private SteamPipelineService pipelineService;

    public GitlabWebHookServiceImpl(DevopsMergeRequestRepository devopsMergeRequestRepository, DevopsGitService devopsGitService, DevopsGitlabCommitService devopsGitlabCommitService,
                                    DevopsGitlabPipelineService devopsGitlabPipelineService, DevOpsCIService devOpsCIService, SteamPipelineService pipelineService) {
        this.devopsMergeRequestRepository = devopsMergeRequestRepository;
        this.devopsGitService = devopsGitService;
        this.devopsGitlabPipelineService = devopsGitlabPipelineService;
        this.devopsGitlabCommitService = devopsGitlabCommitService;
        this.devOpsCIService = devOpsCIService;
        this.pipelineService = pipelineService;
    }

    @Override
    public void forwardingEventToPortal(String body, String token) {
        JsonObject returnData = new JsonParser().parse(body).getAsJsonObject();
        String kind = returnData.get("object_kind").getAsString();
        LOGGER.info("接收到Gitlab Web Hook，kind={}", kind);
        switch (kind) {
            case "merge_request":
                DevopsMergeRequestDTO devopsMergeRequestDTO = JSONArray.parseObject(body, DevopsMergeRequestDTO.class, FastjsonParserConfigProvider.getParserConfig());
                DevopsMergeRequestE devopsMergeRequestE = ConvertHelper.convert(devopsMergeRequestDTO,
                        DevopsMergeRequestE.class);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(devopsMergeRequestE.toString());
                }

                devopsMergeRequestRepository.saveDevopsMergeRequest(devopsMergeRequestE);
                break;
            case "push":
                PushWebHookDTO pushWebHookDTO = JSONArray.parseObject(body, PushWebHookDTO.class, FastjsonParserConfigProvider.getParserConfig());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(pushWebHookDTO.toString());
                }
                pipelineService.sendGitLabWebHook(pushWebHookDTO);
                devopsGitService.branchSync(pushWebHookDTO, token);
                devopsGitlabCommitService.create(pushWebHookDTO, token);
                devOpsCIService.getRepositorySize(pushWebHookDTO.getProject());
                break;
            case "pipeline":
                PipelineWebHookDTO pipelineWebHookDTO = JSONArray.parseObject(body, PipelineWebHookDTO.class, FastjsonParserConfigProvider.getParserConfig());

                devopsGitlabPipelineService.create(pipelineWebHookDTO, token);
                break;
            case "build":
                JobWebHookDTO jobWebHookDTO = JSONArray.parseObject(body, JobWebHookDTO.class, FastjsonParserConfigProvider.getParserConfig());
                devopsGitlabPipelineService.updateStages(jobWebHookDTO);
                break;
            case "tag_push":
                PushWebHookDTO tagPushWebHookDTO = JSONArray.parseObject(body, PushWebHookDTO.class, FastjsonParserConfigProvider.getParserConfig());
                devopsGitlabCommitService.create(tagPushWebHookDTO, token);
                break;
            default:
                break;
        }
    }

    @Override
    public void gitOpsWebHook(String body, String token) {
        JsonObject returnData = new JsonParser().parse(body).getAsJsonObject();
        String kind = returnData.get("object_kind").getAsString();
        if ("push".equals(kind)) {
            PushWebHookDTO pushWebHookDTO = JSONArray.parseObject(body, PushWebHookDTO.class, FastjsonParserConfigProvider.getParserConfig());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(pushWebHookDTO.toString());
            }
            devopsGitService.fileResourceSyncSaga(pushWebHookDTO, token);
            devOpsCIService.getRepositorySize(pushWebHookDTO.getProject());
        }
    }
}
