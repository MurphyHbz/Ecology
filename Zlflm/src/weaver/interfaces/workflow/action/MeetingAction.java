package weaver.interfaces.workflow.action;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import weaver.general.BaseBean;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.DetailTableInfo;
import weaver.soa.workflow.request.MainTableInfo;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;

import java.io.IOException;
import java.util.Properties;

/**
 * @Author liu rui qi
 * @Date 2023-01-13  星期五
 */

public class MeetingAction implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        MainTableInfo mainTableInfo = requestInfo.getMainTableInfo();
        DetailTableInfo detailTableInfo = requestInfo.getDetailTableInfo();
        String src = requestInfo.getRequestManager().getSrc();
        int userId = requestInfo.getRequestManager().getUserId();
        Property[] property = mainTableInfo.getProperty();
//        String field1 = "field98365";//会议id
//        String field2 = "field98363";//备注
        String field1 = "hyid";//会议id
        String field2 = "bz";//备注
        String field3 = "ydr";//预定人
        //请求参数
        String optType = "MS001";
        String hyid = null;
        String status = "1";
        String staffId = "";
        if("reject".equals(src)){
            status = "2";
        }
        String bz = null;
        for (int i = 0; i < property.length; i++) {
            String name = property[i].getName();
            String value = property[i].getValue();
            new BaseBean().writeLog("字段名："+name+",字段值："+value);
            if (field1.equals(name)){
                hyid = value;
            }
            if (field2.equals(name)){
                bz = value;
            }
        }
        staffId = String.valueOf(userId);
        new BaseBean().writeLog("流程状态："+src+"，操作类型："+optType+"，会议id:"+hyid+"，状态："+status+"，审核人员id："+staffId+"，备注："+bz);
        String s = sendPost(optType, hyid, status, staffId, bz);
        new BaseBean().writeLog(s);
        return Action.SUCCESS;
    }
    public String sendPost(String optType,String meetingId,String status,String staffId,String data){
        String url = "http://172.17.140.9:9092/aolsee-meeting/meetingIface/outApi";
        String res = null;
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("OPT_TYPE",optType);
        jsonObject.put( "MEETING_ID",meetingId);
        jsonObject.put( "STATUS",status);
        jsonObject.put("STAFF_ID",staffId);
        jsonObject.put("DATA",data);
        RequestBody body = RequestBody.create(mediaType, jsonObject.toJSONString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            res = response.body().string();
            new BaseBean().writeLog("会议回调请求"+res);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
