package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.CommitFormRecordDTO;
import io.choerodon.devops.api.dto.CommitFormUserDTO;
import io.choerodon.devops.api.dto.DevopsGitlabCommitDTO;
import io.choerodon.devops.api.dto.PushWebHookDTO;
import io.choerodon.devops.app.service.DevopsGitlabCommitService;
import io.choerodon.devops.domain.application.entity.ApplicationE;
import io.choerodon.devops.domain.application.entity.DevopsGitlabCommitE;
import io.choerodon.devops.domain.application.entity.gitlab.CommitE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.repository.ApplicationRepository;
import io.choerodon.devops.domain.application.repository.DevopsGitRepository;
import io.choerodon.devops.domain.application.repository.DevopsGitlabCommitRepository;
import io.choerodon.devops.domain.application.repository.IamRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DevopsGitlabCommitServiceImpl implements DevopsGitlabCommitService {

    private static final Gson gson = new Gson();
    private static final Integer ADMIN = 1;

    @Autowired
    IamRepository iamRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DevopsGitlabCommitRepository devopsGitlabCommitRepository;
    @Autowired
    private DevopsGitRepository devopsGitRepository;

    @Override
    public void create(PushWebHookDTO pushWebHookDTO, String token) {
        ApplicationE applicationE = applicationRepository.queryByToken(token);
        String ref = pushWebHookDTO.getRef().split("/")[2];
        if (!pushWebHookDTO.getCommits().isEmpty()) {
            pushWebHookDTO.getCommits().forEach(commitDTO -> {
                DevopsGitlabCommitE devopsGitlabCommitE = devopsGitlabCommitRepository.queryByShaAndRef(commitDTO.getId(), ref);

                if (devopsGitlabCommitE == null) {
                    devopsGitlabCommitE = new DevopsGitlabCommitE();
                    devopsGitlabCommitE.setAppId(applicationE.getId());
                    devopsGitlabCommitE.setCommitContent(commitDTO.getMessage());
                    devopsGitlabCommitE.setCommitSha(commitDTO.getId());
                    devopsGitlabCommitE.setRef(ref);
                    devopsGitlabCommitE.setUrl(commitDTO.getUrl());
                    if ("root".equals(commitDTO.getAuthor().getName())) {
                        devopsGitlabCommitE.setUserId(1L);
                    } else {
                        UserE userE = iamRepository.queryByEmail(applicationE.getProjectE().getId(),
                                commitDTO.getAuthor().getEmail());
                        if (userE != null) {
                            devopsGitlabCommitE.setUserId(userE.getId());
                        }
                    }
                    devopsGitlabCommitE.setCommitDate(commitDTO.getTimestamp());
                    devopsGitlabCommitRepository.create(devopsGitlabCommitE);
                }
            });
        } else {
            //?????????????????????????????????????????????????????????commits??????
            DevopsGitlabCommitE devopsGitlabCommitE = devopsGitlabCommitRepository.queryByShaAndRef(pushWebHookDTO.getCheckoutSha(), ref);
            if (devopsGitlabCommitE == null) {
                CommitE commitE = devopsGitRepository.getCommit(TypeUtil.objToInteger(applicationE.getGitlabProjectE().getId()), pushWebHookDTO.getCheckoutSha(), ADMIN);
                devopsGitlabCommitE = new DevopsGitlabCommitE();
                devopsGitlabCommitE.setAppId(applicationE.getId());
                devopsGitlabCommitE.setCommitContent(commitE.getMessage());
                devopsGitlabCommitE.setCommitSha(commitE.getId());
                devopsGitlabCommitE.setRef(ref);
                devopsGitlabCommitE.setUrl(commitE.getUrl());
                if ("root".equals(commitE.getAuthorName())) {
                    devopsGitlabCommitE.setUserId(1L);
                } else {
                    UserE userE = iamRepository.queryByEmail(applicationE.getProjectE().getId(),
                            commitE.getAuthorEmail());
                    if (userE != null) {
                        devopsGitlabCommitE.setUserId(userE.getId());
                    }
                }
                devopsGitlabCommitE.setCommitDate(commitE.getCommittedDate());
                devopsGitlabCommitRepository.create(devopsGitlabCommitE);
            }
        }

    }

    @Override
    public DevopsGitlabCommitDTO getCommits(Long projectId, String appIds, Date startDate, Date endDate) {

        List<Long> appIdsMap = gson.fromJson(appIds, new TypeToken<List<Long>>() {
        }.getType());
        if (appIdsMap.isEmpty()) {
            return new DevopsGitlabCommitDTO();
        }

        // ???????????????????????????commit??????
        List<DevopsGitlabCommitE> devopsGitlabCommitES = devopsGitlabCommitRepository
                .listCommits(projectId, appIdsMap, startDate, endDate);
        if (devopsGitlabCommitES.isEmpty()) {
            return new DevopsGitlabCommitDTO();
        }

        // ????????????????????????????????????
        Map<Long, UserE> userMap = getUserDOMap(devopsGitlabCommitES);

        // ?????????????????????commit
        List<CommitFormUserDTO> commitFormUserDTOS = getCommitFormUserDTOList(devopsGitlabCommitES, userMap);

        // ????????????commit(??????????????????commit_date??????????????????)???????????????????????????
        List<Date> totalCommitsDate = getTotalDates(commitFormUserDTOS);
        Collections.sort(totalCommitsDate);

        return new DevopsGitlabCommitDTO(commitFormUserDTOS, totalCommitsDate);
    }

    @Override
    public Page<CommitFormRecordDTO> getRecordCommits(Long projectId, String appIds, PageRequest pageRequest,
                                                      Date startDate, Date endDate) {

        List<Long> appIdsMap = gson.fromJson(appIds, new TypeToken<List<Long>>() {
        }.getType());
        if (appIdsMap.isEmpty()) {
            return new Page<>();
        }

        // ???????????????????????????commit??????
        List<DevopsGitlabCommitE> devopsGitlabCommitES = devopsGitlabCommitRepository
                .listCommits(projectId, appIdsMap, startDate, endDate);
        Map<Long, UserE> userMap = getUserDOMap(devopsGitlabCommitES);
        // ???????????????commit(???????????????commit?????????????????????????????????????????????)
        return getCommitFormRecordDTOS(projectId, appIdsMap, pageRequest, userMap, startDate, endDate);
    }

    private Map<Long, UserE> getUserDOMap(List<DevopsGitlabCommitE> devopsGitlabCommitES) {
        // ??????users
        List<UserE> userEList = iamRepository.listUsersByIds(devopsGitlabCommitES.stream().map(
                DevopsGitlabCommitE::getUserId).distinct().collect(Collectors.toList()));

        return userEList.stream().collect(Collectors.toMap(UserE::getId, u -> u, (u1, u2) -> u1));
    }

    private List<CommitFormUserDTO> getCommitFormUserDTOList(List<DevopsGitlabCommitE> devopsGitlabCommitES,
                                                             Map<Long, UserE> userMap) {
        List<CommitFormUserDTO> commitFormUserDTOS = new ArrayList<>();
        // ??????list???key???userid???value???list
        Map<Long, List<DevopsGitlabCommitE>> map = new HashMap<>();
        for (DevopsGitlabCommitE commitE : devopsGitlabCommitES) {
            Long userId = commitE.getUserId();
            if (userId == null && !map.containsKey(0L)) {
                List<DevopsGitlabCommitE> commitES = new ArrayList<>();
                commitES.add(commitE);
                map.put(0L, commitES);
            } else if (userId == null && map.containsKey(0L)) {
                map.get(0L).add(commitE);
            } else if (userId != null && !map.containsKey(userId)) {
                List<DevopsGitlabCommitE> commitES = new ArrayList<>();
                commitES.add(commitE);
                map.put(userId, commitES);
            } else {
                map.get(userId).add(commitE);
            }
        }
        map.forEach((userId, list) -> {
            UserE userE = userMap.get(userId);
            String name = userE == null ? null : userE.getRealName() + userE.getLoginName();
            String imgUrl = userE == null ? null : userE.getImageUrl();
            // ??????list???????????????????????????commitdate????????????List<Date>??????????????????DTO
            List<Date> date = new ArrayList<>();
            list.forEach(e -> date.add(e.getCommitDate()));
            commitFormUserDTOS.add(new CommitFormUserDTO(userId, name, imgUrl, date));
        });
        return commitFormUserDTOS;
    }

    private Page<CommitFormRecordDTO> getCommitFormRecordDTOS(Long projectId, List<Long> appId, PageRequest pageRequest,
                                                              Map<Long, UserE> userMap, Date startDate, Date endDate) {
        return devopsGitlabCommitRepository.pageCommitRecord(projectId, appId, pageRequest, userMap, startDate, endDate);
    }

    private List<Date> getTotalDates(List<CommitFormUserDTO> commitFormUserDTOS) {
        List<Date> totalCommitsDate = new ArrayList<>();
        commitFormUserDTOS.forEach(e -> totalCommitsDate.addAll(e.getCommitDates()));
        return totalCommitsDate;
    }
}
