package io.choerodon.devops.domain.application.convertor;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.domain.application.entity.CIApplicationE;
import io.choerodon.devops.infra.dataobject.devopsCI.CIApplicationDO;
import org.springframework.beans.BeanUtils;

public class DevopsCIApplicationConvertor implements ConvertorI<CIApplicationE, CIApplicationDO, Object> {

    @Override
    public CIApplicationE doToEntity(CIApplicationDO dataObject) {
        CIApplicationE ciApplicationE = new CIApplicationE();
        BeanUtils.copyProperties(dataObject, ciApplicationE);
        return ciApplicationE;
    }
}
