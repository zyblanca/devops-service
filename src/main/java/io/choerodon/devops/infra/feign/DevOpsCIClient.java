package io.choerodon.devops.infra.feign;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.devops.infra.feign.fallback.DevOpsCIClientFallback;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "devops-ci", fallback = DevOpsCIClientFallback.class)
public interface DevOpsCIClient {

    @PostMapping(value = "v1/git/getRepositorySize")
    ResponseEntity<JSONObject> getRepositorySize(@RequestBody JSONObject parameters);

}
