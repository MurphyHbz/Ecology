package com.engine.workflow.ZLdyc.mys.cmd;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SetProjectAndProjectCompanyGXDataCmd extends AbstractCommonCommand<Map<String, Object>>  {

    private BaseBean bs = new BaseBean();
    Map<String, Object> resultMap = new HashMap<>(2);
    public SetProjectAndProjectCompanyGXDataCmd(Map<String, Object> map) {
        this.params = map;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        bs.writeLog("同步数据Cmd"+this.params);

        JSONObject jsonObject =JSONObject.parseObject(this.params.toString());

        JSONArray list = jsonObject.getJSONArray("Data");

        bs.writeLog("同步数据Cmd-data"+this.params.get("Data"));
        String SyncType = Util.null2String(this.params.get("SyncType"));
        String tableName = bs.getPropValue("interfacet_tablename",SyncType );
        bs.writeLog("同步数据Cmd-SyncType"+SyncType);
        bs.writeLog("同步数据Cmd-tableName"+tableName);
        bs.writeLog("同步数据Cmd-list"+list);
        try {
            if (!list.isEmpty()) {
               Boolean flag=true;
                for (int i = 0; i < list.size(); i++) {
                    bs.writeLog("同步数据Cmd-i"+i);
                    Map<String, Object> hashMap =list.getJSONObject(i);
                    bs.writeLog("同步数据Cmd-hashMap"+hashMap);
                    StringBuffer key = new StringBuffer();
                    String sn="";
                    JSONArray detailList = new JSONArray();
                    for (Map.Entry<String, Object> entry : hashMap.entrySet()) {
                        if (entry.getKey().equals("sn")){
                            sn=entry.getValue().toString();
                        }
                        key.append(entry.getKey()).append(" =  '").append(entry.getValue()).append("' ,");

                    }
                    String substringKey = key.toString().substring(0, key.toString().length() - 1);
                    Boolean aBoolean = updateOrInsert(substringKey, sn, tableName,detailList);
                    if (!aBoolean){
                        flag=false;
                    }
                }
                bs.writeLog("最终flag"+flag);
               if (flag){
                   resultMap.put("code", "2000");
                   resultMap.put("message", "success");
               }else {
                   resultMap.put("code", "4000");
                   resultMap.put("message", "数据同步失败");
               }
            }
        }catch (Exception e){
            resultMap.put("code", "4000");
            resultMap.put("message", e.getMessage());
        }

        return resultMap;
    }



/*
    更新、新增主表数据
 */

    public Boolean updateOrInsert(String sql,String sn,String tableName, JSONArray Datalist){
        RecordSet rs = new RecordSet();
        boolean boo = true;
        Boolean aBoolean =true;
        try{
            //建模表模块的id
            int modeid = 0;
            rs.executeQuery("select id from modeinfo where formid = (select id from workflow_bill where tablename = '" + tableName + "')");
            if (rs.next()) {
                modeid = Integer.parseInt(Util.null2String(rs.getString("id")));
            }
            //单条数据id
            ModeDataIdUpdate modeDataIdUpdate = new ModeDataIdUpdate();
            ModeRightInfo ModeRightInfo = new ModeRightInfo();//调用生成模块权限的方法
            String id = "";
            //先查找该数据是否已存在
            rs.executeQuery("select id from " + tableName + " where sn='"+sn+"'");
            if (rs.next()) {
                bs.writeLog("存在数据-更新数据-sn"+sn);
                id = Util.null2String(rs.getString("id"));
                //更新数据
                boo = rs.executeUpdate("update " + tableName + " set "+sql+" where id = "+id );
                if (!Datalist.isEmpty()){
                   aBoolean = insertDatail(Datalist, id, tableName);
                }

            } else {
                //否则新增数据
                bs.writeLog("新增数据-开始-sn:"+sn);
                Date date = new Date();
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
                id = String.valueOf(modeDataIdUpdate.getModeDataNewId(tableName, modeid, 1, 0, sdfDate.format(date), sdfTime.format(date)));
                boo = rs.executeUpdate("update " + tableName + " set "+sql+" where id = "+id );
                if (!Datalist.isEmpty()){
                     aBoolean = insertDatail(Datalist, id, tableName);
                }
                bs.writeLog("新增某条数据-完成");
            }
            //权限
            ModeRightInfo.editModeDataShare(1, modeid, Integer.parseInt(id));
            bs.writeLog("新增某条数据-重构权限");

        }catch (Exception e){
            bs.writeLog("明细数据新增失败" + e.getMessage());
        }
        bs.writeLog("主表数据flag"+boo +"ssss"+aBoolean);
        return boo&&aBoolean;
    }
    /*
       更新、新增明细数据
     */
    public Boolean insertDatail (JSONArray Datalist ,String id,String tableName){
        RecordSet rs = new RecordSet();
        BaseBean baseBean = new BaseBean();
        Boolean flag=true;
        try {
            for (int i = 0; i < Datalist.size(); i++) {
                StringBuffer detailKey = new StringBuffer();
                StringBuffer detailValue = new StringBuffer();
                Map<String, Object> hashMap = Datalist.getJSONObject(i);

                for (Map.Entry<String, Object> entry : hashMap.entrySet()) {

                    if(entry.getKey().equals("id")){
                        //将传过来的id排除掉
                    }else {
                        detailKey.append(entry.getKey()).append(" ,");
                        detailValue.append("'").append(entry.getValue()).append("'").append(" ,");

                    }
                }

                String substringKeyStr = detailKey.append("mainid").toString();
                String substringValueStr = detailValue.append("'").append(id).append("'").toString();
                //删除再新增
                rs.executeQuery("select * from "+ tableName + "_dt1 where mainid=?", id);
                if(rs.next()){
                    rs.executeUpdate("delete from " + tableName + "_dt1 where mainid=?", id);
                }

                rs.executeQuery("insert into " + tableName + "_dt1 (" + substringKeyStr + ") values (" + substringValueStr + ")");


            }
            baseBean.writeLog("明细数据flag"+flag);

        }catch (Exception e){
            flag=false;
            baseBean.writeLog("明细数据新增失败" + e.getMessage());
        }
        return flag;

    }



    @Override
    public BizLogContext getLogContext() {
        return null;
    }



}


