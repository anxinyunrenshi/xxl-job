package com.xxl.job.admin.core.alarm.dingtalk;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Description:   .
 *
 * @author : yanggang
 * @date : Created in 2018-10-30 0030 11:22
 */
@Slf4j
@Component
public class DingTalkClient implements InitializingBean {

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient okHttpClient;

    public void sendMessage(DingTalkMessageVO dingTalkMessageVO) {
        String context = buildSendMessage(dingTalkMessageVO);

        RequestBody requestBody = RequestBody.create(JSON, context);
        //创建请求
        Request request = new Request.Builder()
                .url(dingTalkMessageVO.getRobotLink())
                .post(requestBody)
                .build();

        //同步发送请求
        Call call = okHttpClient.newCall(request);
        Response response;
        try {
            response = call.execute();
            if (!response.isSuccessful()) {
                log.error("钉钉发送消息失败");
            }
        } catch (Exception e) {
            log.error("钉钉发送消息失败{}", e);
        }
    }

    /**
     * 发送的消息体
     */
    private String buildSendMessage(DingTalkMessageVO dingTalkMessageVO) {

        Map<String, Object> items = new HashMap<>(3);
        items.put("msgtype", "text");
        Map<String, String> textContent = new HashMap<>(1);

        if (dingTalkMessageVO.getContext() == null || dingTalkMessageVO.getContext().trim().length() == 0) {
            throw new IllegalArgumentException("text should not be blank");
        }

        textContent.put("content", dingTalkMessageVO.getContext());
        items.put("text", textContent);
        Map<String, Object> atItems = new HashMap<>(3);
        if (dingTalkMessageVO.getAtMobiles() != null && !dingTalkMessageVO.getAtMobiles().isEmpty()) {
            atItems.put("atMobiles", dingTalkMessageVO.getAtMobiles());
        }

        if (dingTalkMessageVO.getIsAtAll() != null) {
            atItems.put("isAtAll", dingTalkMessageVO.getIsAtAll());
        }

        items.put("at", atItems);
        return com.alibaba.fastjson.JSON.toJSONString(items);
    }

    @Override
    public void afterPropertiesSet() {
        okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

}
