package com.xxl.job.admin.core.thread;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job monitor instance
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
public class JobFailMonitorHelper {
	private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);
	
	private static JobFailMonitorHelper instance = new JobFailMonitorHelper();
	public static JobFailMonitorHelper getInstance(){
		return instance;
	}

	// ---------------------- monitor ----------------------

	private Thread monitorThread;
	private volatile boolean toStop = false;
	public void start(){
		monitorThread = new Thread(new Runnable() {

			@Override
			public void run() {

				// monitor
				while (!toStop) {
					try {

						List<Long> failLogIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findFailJobLogIds(1000);
						if (failLogIds!=null && !failLogIds.isEmpty()) {
							for (long failLogId: failLogIds) {

								// lock log
								int lockRet = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, 0, -1);
								if (lockRet < 1) {
									continue;
								}
								XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(failLogId);
								XxlJobInfo info = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(log.getJobId());

								// 1、fail retry monitor
								if (log.getExecutorFailRetryCount() > 0) {
									JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount()-1), log.getExecutorShardingParam(), log.getExecutorParam());
									String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_type_retry") +"<<<<<<<<<<< </span><br>";
									log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
									XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(log);
								}

								// 2、fail alarm monitor
								int newAlarmStatus = 0;		// 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                                try {
                                    newAlarmStatus = XxlJobAdminConfig.getAdminConfig().getJobAlarmer().alarm(info, log);
                                } catch (Exception e) {
                                    newAlarmStatus = 3;
                                    logger.error(e.getMessage(), e);
                                }

								XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, -1, newAlarmStatus);
							}
						}

					} catch (Exception e) {
						if (!toStop) {
							logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
						}
					}

					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (Exception e) {
						if (!toStop) {
							logger.error(e.getMessage(), e);
						}
					}
				}

				logger.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");

			}
		});
		monitorThread.setDaemon(true);
		monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
		monitorThread.start();
	}

	public void toStop(){
		toStop = true;
		// interrupt and wait
		monitorThread.interrupt();
		try {
			monitorThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
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

    private static final long TRIGGER_TNTERVAL = 3 * 60 * 1000L;

    public boolean isAlarm(Integer key){
		try {
			AlarmCount alarmCount = this.cache.get(key);
			// 触发间隔时间大于三分钟
			if(alarmCount.incrementAndGet() > 1 && (System.currentTimeMillis() - alarmCount.getCreateTime().getTime()) > TRIGGER_TNTERVAL){
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
