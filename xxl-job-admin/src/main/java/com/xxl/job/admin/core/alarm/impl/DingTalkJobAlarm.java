package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.XxlJobAdminApplication;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.alarm.dingtalk.DingTalkComponent;
import com.xxl.job.admin.core.alarm.dingtalk.XxlJobLogConvertor;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Description :   .
 *
 * @author : LikeGirl
 * @date : Created in 2020/4/3 15:49
 */
@Component
public class DingTalkJobAlarm implements JobAlarm {

    private static Logger logger = LoggerFactory.getLogger(DingTalkJobAlarm.class);


    @Value("${ding.talk.alarm.enable:true}")
    private boolean dingTalkAlarmEnable;

    @Override
    public int doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        int result = 1;
        if(!dingTalkAlarmEnable){
            logger.info("钉钉告警已关闭. JobHandler[{}]", jobLog.getExecutorHandler());
            return result;
        }
        if(info != null){
            try {
                result = 2;
                XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(info.getJobGroup());
                DingTalkComponent dingTalkComponent = XxlJobAdminApplication.context.getBean(DingTalkComponent.class);
                dingTalkComponent.sendAlarmNotAll(XxlJobLogConvertor.convert(group, info, jobLog));
            } catch (Exception e) {
                result = 3;
                logger.error("钉钉发送异常: {}", e.getMessage(), e);
            }
        }
        return result;
    }
}
