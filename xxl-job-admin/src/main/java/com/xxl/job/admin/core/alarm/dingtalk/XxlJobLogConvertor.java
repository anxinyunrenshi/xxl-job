package com.xxl.job.admin.core.alarm.dingtalk;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * Description :   .
 *
 * @author : zhangyouwen
 * @date : Created in 2019/9/3 10:07
 */
public class XxlJobLogConvertor {

    private static final String LF = "\n";

    public static String convert(XxlJobGroup jobGroup, XxlJobInfo jobInfo, XxlJobLog jobLog) {
        return new StringBuilder()
                .append("监控告警明细：").append(LF)
                .append("执行器：").append(jobGroup.getTitle()).append(LF)
                .append("任务ID：").append(jobLog.getJobId()).append(LF)
                .append("任务描述：").append(jobInfo.getJobDesc()).append(LF)
                .append("告警类型：调度失败").append(LF)
                .append("告警内容：").append(LF)
                .append("Alarm Job LogId=").append(jobLog.getId()).append(LF)
                .append("TriggerMsg=").append(jobLog.getTriggerMsg().replaceAll("<br>", LF).replace("<span style=\"color:#00c0ef;\" > ", "").replace(" </span>", ""))
                .toString();
    }

    public static void main(String[] args) {
        String s = "任务触发类型：手动触发<br>调度机器：192.168.3.34<br>执行器-注册方式：自动注册<br>执行器-地址列表：null<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\\\"color:#00c0ef;\\\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>调度失败：执行器地址为空<br><br>";
        System.out.println(s.replaceAll("<br>", LF).replace("<span style=\\\"color:#00c0ef;\\\" > ", "").replace(" </span>", ""));


        String s1 = "监控告警明细：\n"
                + "执行器：示例执行器\n"
                + "任务ID：2\n"
                + "任务描述：测试1\n"
                + "告警类型：调度失败\n"
                + "告警内容：\n"
                + "Alarm Job LogId=166\n"
                + "TriggerMsg=任务触发类型：手动触发\n"
                + "调度机器：192.168.3.34\n"
                + "执行器-注册方式：自动注册\n"
                + "执行器-地址列表：null\n"
                + "路由策略：第一个\n"
                + "阻塞处理策略：单机串行\n"
                + "任务超时时间：0\n"
                + "失败重试次数：0\n"
                + "\n"
                + "<span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<<\n"
                + "调度失败：执行器地址为空\n"
                + "\n";

        System.out.println(s1.replace("<span style=\"color:#00c0ef;\" > ", ""));
    }

}
