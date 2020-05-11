package com.xxl.job.admin.core.alarm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);

    private ApplicationContext applicationContext;
    private List<JobAlarm> jobAlarmList;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (serviceBeanMap.size() > 0) {
            jobAlarmList = new ArrayList<JobAlarm>(serviceBeanMap.values());
        }
    }

    /**
     * job alarm
     *
     * @param info      /
     * @param jobLog    /
     * @return          /
     */
    public int alarm(XxlJobInfo info, XxlJobLog jobLog) {

        int result = 1; // success means all-success

        // 避免过多告警
        if(info != null && !isAlarm(info.getId())){
            return result;
        }

        if (jobAlarmList!=null && jobAlarmList.size()>0) {
            for (JobAlarm alarm: jobAlarmList) {
                int resultItem = 1;
                try {
                    resultItem = alarm.doAlarm(info, jobLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                result = alarmResult(result, resultItem);
            }
        }

        return result;
    }

    private int alarmResult(int result,int channelResult){
        // 所有告警成功才是成功
        if(result == 3 || channelResult == 3){
            return 3;
        }
        // 成功
        if(channelResult == 2){
            return 2;
        }

        return result;

    }

    /**
     * 如果在时间窗口 N 内发生了 X 次异常信息，相应的我就需要作出反馈（报警、记录日志等）
     */
    private final LoadingCache<Integer, AlarmCount> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(36, TimeUnit.HOURS)
            .build(new CacheLoader<Integer, AlarmCount>(){
                @Override
                public AlarmCount load(Integer key) throws Exception {
                    return new AlarmCount(key);
                }
            });


    /**
     * 错误次数
     */
    @Value("${xxl.job.alarm.trigger.error-times:-1}")
    private Integer errorTimes;

    /**
     * 时间间隔 单位:毫秒
     * default: 3 * 60 * 1000L
     */
    @Value("${xxl.job.alarm.trigger.time-interval:180000}")
    private Long timeInterval;

    public boolean isAlarm(Integer key){
        try {
            AlarmCount alarmCount = this.cache.get(key);
            // TODO 代码告警不清除 易读
            // 触发次数检验  errorTimes =》 -1 , 即是关闭计数触发的方式
            boolean t1 = errorTimes < 0 ? false : alarmCount.incrementAndGet() > errorTimes;
            // 触发间隔时间检验 timeInterval =》 -1, 即是关闭间隔时间触发的方式
            boolean t2 = timeInterval < 0 ? false : (System.currentTimeMillis() - alarmCount.getCreateTime().getTime()) > timeInterval;
            if(t1 || t2){
                // 删除缓存 进入一下次
                logger.info("触发告警: {}", alarmCount);
                this.cache.invalidate(key);
                return true;
            }
        } catch (ExecutionException e) {
            // no doing anything
        }
        return false;
    }

    public static class AlarmCount {
        private Integer jobId;
        // 计数器
        private AtomicLong count;
        // 上一次触发时间
        private Date lastTriggerTime;
        // 计数开始时间
        private Date createTime;

        public AlarmCount(Integer jobId) {
            this.jobId = jobId;
            this.count = new AtomicLong(0);
            this.createTime = new Date();
        }

        public long incrementAndGet(){
            this.lastTriggerTime = new Date();
            return this.count.incrementAndGet();
        }

        public Integer getJobId() {
            return jobId;
        }

        public AtomicLong getCount() {
            return count;
        }

        public Date getLastTriggerTime() {
            return lastTriggerTime;
        }

        public Date getCreateTime() {
            return createTime;
        }

        @Override
        public String toString() {
            return "AlarmCount{" +
                    "jobId=" + jobId +
                    ", count=" + count +
                    ", lastTriggerTime=" + lastTriggerTime +
                    ", createTime=" + createTime +
                    '}';
        }
    }

}
