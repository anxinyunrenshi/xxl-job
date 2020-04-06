package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return 0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
     */
    public int doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
