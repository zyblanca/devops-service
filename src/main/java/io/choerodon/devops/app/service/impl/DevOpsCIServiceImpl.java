package io.choerodon.devops.app.service.impl;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.devops.api.dto.ProjectWebHookDto;
import io.choerodon.devops.app.service.DevOpsCIService;
import io.choerodon.devops.infra.feign.DevOpsCIClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class DevOpsCIServiceImpl implements DevOpsCIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevOpsCIServiceImpl.class);

    @Autowired
    private DevOpsCIClient devOpsCIClient;

    @Override
    public void getRepositorySize(ProjectWebHookDto project) {
        try {
            if(project == null){
                LOGGER.warn("getRepositorySize Project isEmpty.");
                return;
            }
            /**
             * 字符串起始第一个字符串不为'/'
             * 字符串默认为'/' , '//', '\'
             */
            if(StringUtils.isEmpty(project.getPathWithNamespace())){
                LOGGER.warn("getRepositorySize Project path isEmpty {}",project.getPathWithNamespace());
                return;
            }
            String [] paths = project.getPathWithNamespace().split("/");
            if(ArrayUtils.isEmpty(paths)){
                LOGGER.warn("getRepositorySize Project path isEmpty {}",project.getPathWithNamespace());
                return;
            }
            /**
             * 获取仓库分组以及项目信息
             */
            String groupName = paths[0];
            String projectName = null;
            if(paths.length > 1){
                projectName = paths[1];
            }
            LOGGER.info("Call Remote Service DevOpsCIClient RequestParameter -> groupName:{}, projectName: {}",groupName,projectName);
            ResponseEntity<JSONObject> responseEntity = devOpsCIClient.statisticsGitLibsize(groupName,projectName);
            LOGGER.info("Call Remote Service DevOpsCIClient ResponseParameter -> responseEntity:{}",responseEntity.toString());
        } catch (Exception e){
            LOGGER.error("getRepositorySize error message ->{}",e.getMessage());
        }

    }
}
