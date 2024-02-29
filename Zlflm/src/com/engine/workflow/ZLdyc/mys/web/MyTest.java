package com.engine.workflow.ZLdyc.mys.web;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import org.junit.Before;
import org.junit.Test;

import org.springframework.web.context.WebApplicationContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
/**
 * Created by crazyDream on 2017/1/10.
 * 测试例子
 */

public class MyTest   {
    /**
     * 获取 token
     */
    @Test
    public void getToken() {
        String url = "http://127.0.0.1:8080/ssologin/getToken";
        Map<String, String> params = new HashMap<>();
        params.put("appid", "ssss");
        params.put("loginid", "fw0011");
        String s = MyTest.doPost(url, params);
        System.out.println(s);
//获得的 token:  659C43561254716F8961A2586888BD17288178101C86582821E50E190F941A72
    }
    /**
     * 带上 token 访问 ecology 页面
     */
    @Test
    public void visitPage() {
        String token ="659C43561254716F8961A2586888BD17288178101C86582821E50E190F941A72";
        String url = "http://127.0.0.1:8080/systeminfo/version.jsp";
        Map<String, String> params = new HashMap<>();
        params.put("ssoToken", token);
        String s = MyTest.doPost(url, params);
        System.out.println(s); // 能成功返回内容
    }


    /**
     * post 请求(用于 key-value 格式的参数)
     * @param url
     * @param params
     * @return
     */
    public static String doPost(String url, Map params){
        BufferedReader in = null;
        try {
            // 定义 HttpClient
            HttpClient client = new DefaultHttpClient();
            // 实例化 HTTP 方法
            HttpPost request = new HttpPost();
            request.setURI(new URI(url));
            //设置参数
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                String value = String.valueOf(params.get(name));
                nvps.add(new BasicNameValuePair(name, value));
                //System.out.println(name +"-"+value);
            }
            request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if(code == 200){ //请求成功
                in = new BufferedReader(new InputStreamReader(response.getEntity()
                        .getContent(),"utf-8"));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }
                in.close();
                return sb.toString();
            }
            else{ //
                System.out.println("状态码：" + code);
                return null;
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    /**
     * post 请求（用于请求 json 格式的参数）
     * @param url
     * @param params
     * @return
     */
    public static String doPost(String url, String params) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);// 创建 httpPost
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");
        String charSet = "UTF-8";
        StringEntity entity = new StringEntity(params, charSet);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpPost);
            StatusLine status = response.getStatusLine();
            int state = status.getStatusCode();
            if (state == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                String jsonString = EntityUtils.toString(responseEntity);
                return jsonString;
            }
            else{
            }
        }
        finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}