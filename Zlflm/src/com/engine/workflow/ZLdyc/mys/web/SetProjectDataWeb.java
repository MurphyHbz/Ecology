package com.engine.workflow.ZLdyc.mys.web;

import com.alibaba.fastjson.JSONObject;
import com.engine.common.util.ServiceUtil;
import com.engine.workflow.ZLdyc.mys.service.impl.SetProjectDataServiceImpl;
import weaver.general.BaseBean;
import weaver.rest.servlet.util.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class SetProjectDataWeb {

    private SetProjectDataServiceImpl getService() {
        return ServiceUtil.getService(SetProjectDataServiceImpl.class);
    }

    /**
     * 主数据信息同步调用接口
     *
     * @return
     */
    @POST
    @Path("/saveData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String saveFile(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apiData = new HashMap<>();
        BaseBean baseBean = new BaseBean();
        try {
            //ParamUtil.request2Map(request);
            String jsonStr = ServletUtil.getServletInputStreamContent(request, "UTF-8");
            baseBean.writeLog("同步数据-jsonStr"+jsonStr);
            Map<String, Object> params = JSONObject.parseObject(jsonStr);
            baseBean.writeLog("同步数据-param"+params);
            Map<String, Object> map = this.getService().saveData(params);
            apiData.putAll(map);
        } catch (Exception e) {
            e.printStackTrace();
            apiData.put("code", "4000");
            apiData.put("message", "catch exception : " + e.getMessage());
        }
        return JSONObject.toJSONString(apiData);

    }

}
