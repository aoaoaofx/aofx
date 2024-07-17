package com.zx.jdkill.test;

import com.alibaba.fastjson.JSONObject;
import com.sun.webkit.network.CookieManager;
import org.quartz.SchedulerException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhaoxu
 * @date: 2021/1/8 20:59
 */
public class Start {
    final static String HEADER_AGENT = "User-Agent";
    final static String HEADER_AGENT_ARG = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36";
    final static String REFERER = "Referer";
    final static String REFERER_ARG = "https://passport.jd.com/new/login.aspx";
    //商品id
    static String pid = "";
    //eid
    static String eid = "";
    static String payPassword = "";
    //fp
    static String fp = "";
    //抢购数量
    static Integer ok = 2;
    //获取ip代理
    static String getIpUrl = "";

    static CookieManager manager = new CookieManager();

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, ParseException {
        initData();
        CookieHandler.setDefault(manager);
        //登录
        Login.Login();

        // 设置目标任务
        judgePruchase();
    }

    public static void initData() throws IOException {
        String fileData = readFile("initData.txt").toString();
        System.out.println(fileData);

        try {
            pid = fileData.split("pid=")[1].split(";")[0];
            eid = fileData.split("eid=")[1].split(";")[0];
            fp = fileData.split("fp=")[1].split(";")[0];
//            ok = Integer.valueOf(fileData.split("ok=")[1].split(";")[0]);
            getIpUrl = fileData.split("getIpUrl=")[1].split(";")[0];
            payPassword = fileData.split("payPassword=")[1].split(";")[0];
        } catch (Exception e) {
            System.out.println("参数错误，每个参数后面需要加分号");
        }

    }

    public static void judgePruchase() throws IOException, ParseException {
        //获取开始时间
        JSONObject headers = new JSONObject();
        headers.put(HEADER_AGENT, HEADER_AGENT_ARG);
        headers.put(REFERER, REFERER_ARG);
        JSONObject shopDetail = JSONObject.parseObject(HttpUrlConnectionUtil.get(headers, "https://item-soa.jd.com/getWareBusiness?skuId=" + pid));
        BaseQuartzManager baseQuartzManager;
        try {
            baseQuartzManager = new BaseQuartzManager();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
        if (shopDetail.get("yuyueInfo") != null) {
            String buyDate = JSONObject.parseObject(shopDetail.get("yuyueInfo").toString()).get("buyTime").toString();
            String startDate = buyDate.split("-202")[0] + ":00";
            long startTime = HttpUrlConnectionUtil.dateToTime(startDate);
            if (startTime <= System.currentTimeMillis()) {
                for (int i = 0; i < 5; i++) {
                    baseQuartzManager.createJob(RushToPurchase.class, "RushToPurchase-" + i, "RushToPurchase",
                            TimeUtil.formatDateByPattern(new Date(), TimeUtil.PATTERN_TARGET_TIME), new JSONObject(), true);
                }
            } else {
                //获取京东时间
                JSONObject jdTime = JSONObject.parseObject(HttpUrlConnectionUtil.get(headers, "https://api.m.jd.com/client.action?functionId=queryMaterialProducts&client=wh5"));
                long serverTime = Long.parseLong(jdTime.get("currentTime2").toString());
                for (int i = 0; i < 100; i++) {
                    int random = (int) (Math.random() * (40 - 5 + 1)) + 5;
                    long localStartTime = startTime - serverTime + System.currentTimeMillis() - random;
                    String cornExpression = TimeUtil.formatDateByPattern(new Date(localStartTime), TimeUtil.PATTERN_TARGET_TIME);
                    baseQuartzManager.createJob(RushToPurchase.class, "RushToPurchase-" + i, "RushToPurchase",
                            cornExpression, new JSONObject(), true);
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                baseQuartzManager.createJob(RushToPurchase.class, "RushToPurchase-" + i, "RushToPurchase",
                        TimeUtil.formatDateByPattern(new Date(), TimeUtil.PATTERN_TARGET_TIME), new JSONObject(), true);
            }
        }
    }

    /**
     * 按行读取全部文件数据
     *
     * @param strFile
     */
    public static StringBuffer readFile(String strFile) throws IOException {
        StringBuffer strSb = new StringBuffer();
        InputStreamReader inStrR = new InputStreamReader(new FileInputStream(strFile), "UTF-8");
        // character streams
        BufferedReader br = new BufferedReader(inStrR);
        String line = br.readLine();
        while (line != null) {
            strSb.append(line).append("\r\n");
            line = br.readLine();
        }
        return strSb;
    }

    /*
     * 中文转unicode编码
     */
    public static String gbEncoding(final String gbString) {
        char[] utfBytes = gbString.toCharArray();
        String unicodeBytes = "";
        for (int i = 0; i < utfBytes.length; i++) {
            String hexB = Integer.toHexString(utfBytes[i]);
            if (hexB.length() <= 2) {
                hexB = "00" + hexB;
            }
            unicodeBytes = unicodeBytes + "\\u" + hexB;
        }
        return unicodeBytes;
    }

    /*
     * unicode编码转中文
     */
    public static String decodeUnicode(final String dataStr) {
        int start = 0;
        int end = 0;
        final StringBuffer buffer = new StringBuffer();
        while (start > -1) {
            end = dataStr.indexOf("\\u", start + 2);
            String charStr = "";
            if (end == -1) {
                charStr = dataStr.substring(start + 2, dataStr.length());
            } else {
                charStr = dataStr.substring(start + 2, end);
            }
            char letter = (char) Integer.parseInt(charStr, 16); // 16进制parse整形字符串。
            buffer.append(new Character(letter).toString());
            start = end;
        }
        return buffer.toString();
    }
}
