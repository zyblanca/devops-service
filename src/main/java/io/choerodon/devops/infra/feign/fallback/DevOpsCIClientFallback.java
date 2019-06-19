package io.choerodon.devops.infra.feign.fallback;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.devops.domain.application.valueobject.CIApplication;
import io.choerodon.devops.infra.feign.DevOpsCIClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DevOpsCIClientFallback implements DevOpsCIClient {

    @Override
    public ResponseEntity<JSONObject> statisticsGitLibsize(String groupName, String projectName) {
        return new ResponseEntity("error.statisticsSize", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<CIApplication> getApplicationByGitAddress(String gitAddress) {
        return new ResponseEntity("查询 devops-ci 应用异常", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
