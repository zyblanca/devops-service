package io.choerodon.devops.infra.feign.fallback;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.devops.infra.feign.DevOpsCIClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DevOpsCIClientFallback implements DevOpsCIClient {

    @Override
    public ResponseEntity<JSONObject> getRepositorySize(JSONObject parameters) {
        return new ResponseEntity("error.repositorySize.get",HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
