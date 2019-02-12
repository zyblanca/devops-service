package io.choerodon.devops.domain.application.convertor;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.infra.dataobject.iam.UserDO;

/**
 * Created by Zenger on 2018/4/16.
 */
@Service
public class IamUserConvertor implements ConvertorI<UserE, UserDO, Object> {

    @Override
    public UserE doToEntity(UserDO dataObject) {
        UserE userE = new UserE();
        BeanUtils.copyProperties(dataObject, userE);
        return userE;
    }
}
