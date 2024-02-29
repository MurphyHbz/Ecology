package weaver.interfaces.workflow.action;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.schedule.BaseCronJob;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author liu rui qi
 * @Date 2023-04-13  星期四
 */

public class ThreeReconciliationJob extends BaseCronJob {

    private String firstDayOfMonth;
    private String lastDayOfMonth;
    private String thisYearAndMonth;
    private String showLog;
    BaseBean log = new BaseBean();

    @Override
    public void execute() {
        log.writeLog("-----------------------ThreeReconciliationJob.execute()定时任务开始执行-------------------------");
        // 开始时间
        long stime = System.currentTimeMillis();
        //TODO 获取当前月份的第一天、最后一天和当前年月
        if(isEmpty(firstDayOfMonth)){
            this.firstDayOfMonth = (String) getThisMonthDay().get("firstDayOfMonth");
        }
        if(isEmpty(lastDayOfMonth)){
            this.lastDayOfMonth = (String) getThisMonthDay().get("lastDayOfMonth");
        }
        if(isEmpty(thisYearAndMonth)){
            this.thisYearAndMonth = (String) getThisMonthDay().get("thisYearAndMonth");
        }
        log.writeLog("本月第一天:"+firstDayOfMonth);
        log.writeLog("本月最后一天:"+lastDayOfMonth);
        log.writeLog("当前年月:"+thisYearAndMonth);
        log.writeLog("showLog:"+showLog);
        //TODO 获取modeid
        RecordSet rs = new RecordSet();
        String tableName = "uf_sfdzdeb";
        String modeName = "三方对账第二版";
        //String sql = "select id from modeinfo where formid=(select id from workflow_bill where tablename='"+tableName+"')and modename = '"+modeName+"'";
        //rs.executeQuery(sql);
        //TODO 防止sql注入
        String sql = "select id from modeinfo where formid=(select id from workflow_bill where tablename= ? )and modename = ?";
        rs.executeQuery(sql,tableName,modeName);

        log.writeLog("三方对账sql:"+sql);

        rs.next();
        int modeid = rs.getInt(1);
        //获取付账款主体信息和流水号
        List<Fkzt> paymentSubjectData = getPaymentSubjectData();
        //获取billId
        ModeDataIdUpdate modeDataIdUpdate = new ModeDataIdUpdate();
        int billId = modeDataIdUpdate.getModeDataNewId(tableName, modeid, 1, 0, getCurrentDate("D"), getCurrentDate("T"));
        //插入主表
        insertMainTable(billId,thisYearAndMonth);
        //批量插入明细表
        insertDetailTableBatch(billId,paymentSubjectData);
        log.writeLog("执行批量插入明细表操作");

        //权限重构
        ModeRightInfo mri = new ModeRightInfo();
        mri.setNewRight(true);
        mri.editModeDataShare(1,modeid,billId);
        // 结束时间
        long etime = System.currentTimeMillis();
        log.writeLog("执行时长："+(etime - stime)+" 毫秒。");
        log.writeLog("-----------------------ThreeReconciliationJob.execute()定时任务结束执行-------------------------");

    }

    /**
     * 一个流水比较三方金额
     * @param bzAmount
     * @param cwAmount
     * @param zfAmount
     * @return
     */
    private boolean compareThreeAmounts(Float bzAmount,Float cwAmount,Float zfAmount) {
        if(bzAmount.compareTo(cwAmount)!=0){
            return false;
        }
        if (bzAmount.compareTo(zfAmount)!=0){
            return false;
        }
        return true;
    }
    /**
     * 比较三个金额是否相等，使用BigDecimal进行计算和比较
     *
     * @param amount1 第一个金额
     * @param amount2 第二个金额
     * @param amount3 第三个金额
     * @param scale   精度位数
     * @return 三个金额是否相等
     */
    private boolean compareThreeAmounts(Float amount1, Float amount2, Float amount3, int scale) {
        BigDecimal bdAmount1 = new BigDecimal(amount1.toString());
        BigDecimal bdAmount2 = new BigDecimal(amount2.toString());
        BigDecimal bdAmount3 = new BigDecimal(amount3.toString());
        bdAmount1 = bdAmount1.setScale(scale, RoundingMode.HALF_UP);
        bdAmount2 = bdAmount2.setScale(scale, RoundingMode.HALF_UP);
        bdAmount3 = bdAmount3.setScale(scale, RoundingMode.HALF_UP);

        return bdAmount1.compareTo(bdAmount2) == 0 && bdAmount1.compareTo(bdAmount3) == 0 && bdAmount2.compareTo(bdAmount3) == 0;
    }
    /**
     * 插入三方对账主表
     * @param id    主表id
     * @param ny    年月
     */
    public boolean insertMainTable(Integer id,String ny){
        //String sql = "update uf_sfdzdeb set NY = '"+ny+"' where id = "+id;
        RecordSet rs = new RecordSet();
        String sql = "update uf_sfdzdeb set NY = ? where id = ?";
        return rs.executeUpdate(sql,ny,id);
    }

    /**
     * 批量插入明细表
     * @param billId
     * @param paymentSubjectData
     */
    private void insertDetailTableBatch(int billId, List<Fkzt> paymentSubjectData) {
        List<List> detail1 = new ArrayList<>();
        List<List> detail2 = new ArrayList<>();

        for (Fkzt datum : paymentSubjectData) {
            Map<String, Object> insertData = getInsertData(firstDayOfMonth, lastDayOfMonth, datum.lsh);
            //Integer mainId,Integer paymentSubject,String lsh,Double bzje,Double cwje,Double zfje
            Float bzAmount = (Float) insertData.get("bzAmount");
            Float cwAmount = (Float) insertData.get("cwAmount");
            Float zfAmount = (Float) insertData.get("zfAmount");
            String retmsg = (String) insertData.get("retmsg");
            String bz = (String) insertData.get("bz");

            List columns = new ArrayList<>();
            
            columns.add(billId);
            columns.add(datum.lsh);
            columns.add(bzAmount);
            columns.add(cwAmount);
            columns.add(zfAmount);
            columns.add(datum.fkztId);
            if(!compareThreeAmounts(bzAmount,cwAmount,zfAmount,3)){
                //差异数据
                detail2.add(new ArrayList<>(columns));
            }
            columns.add(retmsg);
            columns.add(bz);
            detail1.add(new ArrayList<>(columns));
        }
        if("1".equals(showLog)){
            log.writeLog("批量数据："+detail1);
        }
        boolean res = insertDetail1Batch(detail1);
        if (res){
            log.writeLog("明细表1批量插入成功");
        }else {
            log.writeLog("明细表1批量插入失败");
        }
        res = insertDetail2Batch(detail2);
        if (res) {
            log.writeLog("明细表2批量插入成功");
        }else {
            log.writeLog("明细表2批量插入失败");
        }
    }

    /**
     * 批量插入明细表1
     * @param params
     * @return
     */
    public boolean insertDetail1Batch(List<List> params){
        RecordSet rs = new RecordSet();
        String sql = "insert into UF_SFDZDEB_DT1(MAINID,LSH,BZXTJE,CWXTJE,ZFXTJE,FKZT,ZFXTFKZT,BZ) VALUES(?,?,?,?,?,?,?,?)";
        log.writeLog("明细表1批量插入sql:"+sql);
        boolean res = false;
        try {
            res = rs.executeBatchSql(sql,params);
        }catch (Exception e){
            log.writeLog("detail1_sql_exception:",e);
        }
        return res;
    }
    /**
     * 批量插入明细表2
     * @param params
     * @return
     */
    public boolean insertDetail2Batch(List<List> params){
        RecordSet rs = new RecordSet();
        String sql = "insert into UF_SFDZDEB_DT2(MAINID,LSH,BZXTJE,CWXTJE,ZFXTJE,FKZT) VALUES(?,?,?,?,?,?)";
        log.writeLog("明细表2批量插入sql:"+sql);
        boolean res = false;
        try {
            res = rs.executeBatchSql(sql,params);
        }catch (Exception e){
            log.writeLog("detail2_sql_exception:",e);
        }
        return res;
    }
    /**
     * 获取所有付款主体id和流水号
     * @return paymentSubjectDataList id按照从小到大顺序排列
     */
    public List<Fkzt> getPaymentSubjectData(){
        List<Fkzt> paymentSubjectDataList = new ArrayList<>();
        RecordSet rs = new RecordSet();
        String getPaymentSubjectDataSql = "select fkzt,lsh from uf_bzxtlsh";
        rs.executeQuery(getPaymentSubjectDataSql);
        while (rs.next()){
            Integer fkzt = rs.getInt("fkzt");
            String lsh = rs.getString("lsh");
            paymentSubjectDataList.add(new Fkzt(fkzt,lsh));
        }
        return paymentSubjectDataList;
    }

    /**
     * 根据流水号查询当月三个系统的金额
     * @param firstDayOfMonth
     * @param lastDayOfMonth
     * @param lsh
     * @return
     */
    private Map<String,Object> getInsertData(String firstDayOfMonth,String lastDayOfMonth,String lsh){
        Map<String,Object> dataMap = new HashMap<>();//数据Map
        //报账系统 type = 1
        String bzSql = "SELECT lsh,fkzt,ybzje as amount,rq FROM uf_bzxtlsh WHERE LSH = '"+lsh+"' and rq BETWEEN '"+firstDayOfMonth+"' AND '"+lastDayOfMonth+"'";
        Map<String, Object> bzMap = findReimbursementInfo(bzSql,1);
        //财务系统 type = 2
        String cwSql = "select t2.lsh,t2.fkzt,t1.rmbjeze as amount,t2.rq from OA_EAS_tmp t1 LEFT JOIN uf_bzxtlsh t2 ON SUBSTR(t1.lsh, 4, 100) = t2.lsh where t2.lsh = '"+lsh+"' and t2.rq BETWEEN '"+firstDayOfMonth+"' AND '"+lastDayOfMonth+"'";
        Map<String, Object> cwMap = findReimbursementInfo(cwSql,2);
        //支付系统 type = 3
        String zfSql = "SELECT t1.lsh,t2.fkzt,t1.YBZJE as amount,t2.rq,t1.RETMSG,t2.BZ FROM uf_yqfk t1 LEFT JOIN uf_bzxtlsh t2 ON t1.lsh = t2.lsh where t2.lsh = '"+lsh+"' and t2.rq BETWEEN '"+firstDayOfMonth+"' AND '"+lastDayOfMonth+"'";
        Map<String, Object> zfMap = findReimbursementInfo(zfSql,3);

        dataMap.put("bzAmount",bzMap.get("amount"));
        dataMap.put("cwAmount",cwMap.get("amount"));
        dataMap.put("zfAmount",zfMap.get("amount"));
        dataMap.put("retmsg",zfMap.get("retmsg"));
        dataMap.put("bz",zfMap.get("bz"));
        if ("1".equals(showLog)){
            log.writeLog("根据流水号查询当月三个系统的金额:"+dataMap);
        }
        return dataMap;
    }
    /**
     *
     * @param sql 执行的sql
     * @param type 1:报账系统,2:财务系统,3:支付系统
     * @return
     */
    private Map<String,Object> findReimbursementInfo(String sql,int type){
        Map<String,Object> resultMap = new HashMap<>();
        RecordSet rs = new RecordSet();
        rs.executeQuery(sql);
        rs.next();
        String lsh = Util.null2String(rs.getString("lsh"));
        Integer fkzt = Util.getIntValue(rs.getString("fkzt"));
        Float amount = Util.getFloatValue(rs.getString("amount"));
        String rq = Util.null2String(rs.getString("rq"));
        if (type==3){
            String retmsg = Util.null2String(rs.getString("retmsg"));
            String bz = Util.null2String(rs.getString("bz"));
            resultMap.put("retmsg",retmsg);
            resultMap.put("bz",bz);
        }
        resultMap.put("lsh",lsh);
        resultMap.put("fkzt",fkzt);
        resultMap.put("amount",amount);
        resultMap.put("rq",rq);
        return resultMap;
    }
    /**
     * 获取本月的第一天、最后一天和当前年月
     * @return resultMap:日期信息  firstDayOfMonth:本月第一天  lastDayOfMonth:本月最后一天  thisYearAndMonth:当前年月
     */
    private Map<String,Object> getThisMonthDay(){
        Map<String,Object> resultMap = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startDay = sdf.format(new Date());

        Calendar calendar = Calendar.getInstance();
        //当前月的第一天
        calendar.set(Integer.parseInt(startDay.substring(0,4)), Integer.parseInt(startDay.substring(5,7)) - 1, 1);
        String firstDayOfMonth = new SimpleDateFormat( "yyyy-MM-dd ").format(calendar.getTime());

        //设置要获取月份的下月的第一天
        calendar.set(Integer.parseInt(startDay.substring(0,4)), Integer.parseInt(startDay.substring(5,7)), 1);
        //将日期值减去一天，从而获取到要求的月份最后一天
        calendar.add(Calendar.DATE, -1);
        String lastDayOfMonth = new SimpleDateFormat( "yyyy-MM-dd ").format(calendar.getTime());

        //获取当前年月yyyyMM
        String year = firstDayOfMonth.substring(0,4);
        String month = firstDayOfMonth.substring(5,7);
        String thisYearAndMonth = year + month;

        resultMap.put("firstDayOfMonth",firstDayOfMonth);
        resultMap.put("lastDayOfMonth",lastDayOfMonth);
        resultMap.put("thisYearAndMonth",thisYearAndMonth);
        return  resultMap;
    }
    public String getCurrentDate(String type) {
        Calendar ca = Calendar.getInstance();
        SimpleDateFormat sdf;

        if ("T".equals(type)) {
            sdf = new SimpleDateFormat("HH:mm:ss");
        } else if ("D".equals(type)) {// 按照默认格式
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        } else if ("NY".equals(type)) {// 按照默认格式
            sdf = new SimpleDateFormat("yyyy-MM");
        } else {
            sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
        }
        return sdf.format(ca.getTime());
    }
    private static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
    public String getFirstDayOfMonth() {
        return firstDayOfMonth;
    }

    public void setFirstDayOfMonth(String firstDayOfMonth) {
        this.firstDayOfMonth = firstDayOfMonth;
    }

    public String getLastDayOfMonth() {
        return lastDayOfMonth;
    }

    public void setLastDayOfMonth(String lastDayOfMonth) {
        this.lastDayOfMonth = lastDayOfMonth;
    }

    public String getThisYearAndMonth() {
        return thisYearAndMonth;
    }

    public void setThisYearAndMonth(String thisYearAndMonth) {
        this.thisYearAndMonth = thisYearAndMonth;
    }


    public String getShowLog() {
        return showLog;
    }

    public void setShowLog(String showLog) {
        this.showLog = showLog;
    }

    private class Fkzt{
        Integer fkztId;
        String lsh;
        public Fkzt(Integer fkztId, String lsh) {
            this.fkztId = fkztId;
            this.lsh = lsh;
        }
    }
}
