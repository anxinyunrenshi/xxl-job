package com.xxl.job.admin.core.alarm.dingtalk;

import com.huixian.common2.util.EnvironmentUtil;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 */
@Slf4j
@Component
public class DingTalkComponent {

    @Value("${alarm.ding-talk.robot.link:}")
    private String robotLink;

    @Autowired
    private DingTalkClient dingTalkClient;

    /**
     * 发送告警通知
     */
    public void sendAlarm(String template, Object... objects) {
        sendAlarm(template, true, objects);
    }

    /**
     * 发送告警通知(不@所有人)
     */
    public void sendAlarmNotAll(String template, Object... objects) {
        sendAlarm(template, false, objects);
    }

    /**
     * 发送告警通知
     */
    public void sendAlarm(String template, Boolean atAll, Object... objects) {
        try {

            // 没配置机器人link，则不发送钉钉告警
            if(robotLink == null || robotLink.trim().length() == 0){
                return;
            }

            String sign = "<xxl-job>";

            if (EnvironmentUtil.isLocOrDev()) {
                log.info("开发环境不发送告警");
                return;
            }

            DingTalkMessageVO dingTalkMessageVO = new DingTalkMessageVO();
            dingTalkMessageVO.setIsAtAll(true);

            if (EnvironmentUtil.isTest()) {
                sign = " <fat>" + sign;
                dingTalkMessageVO.setIsAtAll(false);
            }

            if (EnvironmentUtil.isSandbox()) {
                sign = " <sandbox>" + sign;
            }

            String context = MessageFormat.format(sign + template, objects);
            dingTalkMessageVO.setContext(context);
            dingTalkMessageVO.setRobotLink(robotLink);
            dingTalkMessageVO.setIsAtAll(atAll);
            log.info("发送告警:{}", context);
            dingTalkClient.sendMessage(dingTalkMessageVO);
        } catch (Exception e) {
            log.error("告警发送异常", e);
        }
    }

}
