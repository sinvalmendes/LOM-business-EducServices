package com.nanuvem.lom.business.attribute;

import com.nanuvem.lom.api.Facade;
import com.nanuvem.lom.api.tests.attribute.LongTextAttributeTest;
import com.nanuvem.lom.business.BusinessFacade;
import com.nanuvem.lom.kernel.dao.MemoryDaoFactory;

public class BusinessLongTextAttributeTest extends LongTextAttributeTest {

    @Override
    public Facade createFacade() {
        return new BusinessFacade(new MemoryDaoFactory());
    }

}
