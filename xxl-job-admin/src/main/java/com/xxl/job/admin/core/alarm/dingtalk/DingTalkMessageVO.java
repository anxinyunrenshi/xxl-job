package com.xxl.job.admin.core.alarm.dingtalk;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author Administrator
 */
@Data
public class DingTalkMessageVO implements Serializable {

    private static final long serialVersionUID = -6651123930369854252L;

    /**
     * 发送内容
     */
    private String context;

    /**
     * 指定要@的人
     */
    private List<String> atMobiles;

    /**
     * 是否@所有人
     */
    private Boolean isAtAll;

    /**
     * 机器人链接
     */
    private String robotLink;

}
