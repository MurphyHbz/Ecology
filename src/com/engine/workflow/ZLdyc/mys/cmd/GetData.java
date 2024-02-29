package com.engine.workflow.ZLdyc.mys.cmd;

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
import java.util.Map;

public class GetData extends AbstractCommonCommand<Map<String, Object>>{
    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        String tableName = "uf_project";
        Boolean falg=true;
        String str = "[{\"name\":\"tom\"},{\"name\":\"tim\"},{\"name\":\"jim\"}]";
        return null;
    }

    public Boolean updateOrInsert(String sql,String sn,String tableName,String value){
        RecordSet rs = new RecordSet();
        BaseBean bs = new BaseBean();
        //建模表模块的id
        int modeid = 0;
        rs.executeQuery("select id from modeinfo where formid = (select id from workflow_bill where tablename = '" + tableName + "')");
        if (rs.next()) {
            modeid = Integer.parseInt(Util.null2String(rs.getString("id")));
        }
        //单条数据id
        //生成新的主表id
        ModeDataIdUpdate modeDataIdUpdate = new ModeDataIdUpdate();
        ModeRightInfo ModeRightInfo = new ModeRightInfo();//调用生成模块权限的方法
        String id = "";
        boolean boo = false;
        //先查找该数据是否已存在
        rs.executeQuery("select id from " + tableName + " where sn='"+sn+"'");
        if (rs.next()) {
            bs.writeLog("存在数据-更新数据");
            id = Util.null2String(rs.getString("id"));
            //更新数据
            boo = rs.executeUpdate("update " + tableName + " set "+sql+" where id = "+id ,value);

        } else {
            //否则新增数据
            bs.writeLog("新增数据-开始");
            Date date = new Date();
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
            id = String.valueOf(modeDataIdUpdate.getModeDataNewId(tableName, modeid, 1, 0, sdfDate.format(date), sdfTime.format(date)));
            boo = rs.executeUpdate("update " + tableName + " set "+sql+" where id = "+id ,value,id);
            bs.writeLog("新增某条数据-完成");
        }

        //权限
        ModeRightInfo.editModeDataShare(1, modeid, Integer.parseInt(id));
        bs.writeLog("新增某条数据-重构权限");

        return boo;
    }
}
