package io.choerodon.devops.infra.feign;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.devops.infra.feign.fallback.DevOpsCIClientFallback;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "devops-ci", fallback = DevOpsCIClientFallback.class)
public interface DevOpsCIClient {

    @RequestMapping(value = {"v1/git/groups/{groupName}/statistic/size"}, method = RequestMethod.GET)
    ResponseEntity<JSONObject> statisticsGitLibsize(@PathVariable(value = "groupName") String groupName,
                                                        @RequestParam(value = "projectName",required = false) String projectName);

}
