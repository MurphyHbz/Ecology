package com.engine.workflow.ZLdyc.mys.service.impl;

import com.engine.core.impl.Service;
import com.engine.workflow.ZLdyc.mys.cmd.*;
import com.engine.workflow.ZLdyc.mys.service.SetProjectDataService;

import java.util.Map;

public class SetProjectDataServiceImpl extends Service implements SetProjectDataService {
    @Override
    public Map<String, Object> saveData(Map<String, Object> map) {
        return this.commandExecutor.execute(new SetSubjectsAndFunctionsDataCmd(map));
    }


}
