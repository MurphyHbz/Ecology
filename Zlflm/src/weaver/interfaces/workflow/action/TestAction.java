package weaver.interfaces.workflow.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.*;


import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class TestAction implements Action {

    @Override
    public String execute(RequestInfo requestInfo) {

        MainTableInfo mainTableInfo = requestInfo.getMainTableInfo();
        DetailTableInfo detailTableInfo = requestInfo.getDetailTableInfo();
        BaseBean bs = new BaseBean();
        RecordSet rs = new RecordSet();
        Map<String,Object>  map = new HashMap<>();
        bs.writeLog("-------------------------开始打印主表数据---------------------------------");
        for (Property property : mainTableInfo.getProperty()) {
            if (property.getName().equals("s")){
                rs.executeQuery("select max(imagefileid) imagefileid from docimagefile where docid = "+property.getValue());
            }
            if (rs.next()){

                
            }
            map.put(property.getName(),property.getValue());

        }
        bs.writeLog("-------------------------打印主表数据结束---------------------------------");

        bs.writeLog("-------------------------开始打印明细表数据---------------------------------");
        DetailTable[] detailTable = detailTableInfo.getDetailTable();
        for (DetailTable table : detailTable) {
            Row[] row = table.getRow();
            for (Row row1 : row) {
                Cell[] cell = row1.getCell();
                for (Cell cell1 : cell) {
                    String name = cell1.getName();
                    String value = cell1.getValue();
                    bs.writeLog(name);
                    bs.writeLog(value);
                }
            }
        }
        bs.writeLog("-------------------------打印明细表数据结束---------------------------------");
        return null;
    }
}
