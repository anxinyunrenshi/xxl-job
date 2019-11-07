package com.xxl.job.admin.controller.extent;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.glue.GlueTypeEnum;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Description :   .
 *
 * @author : LikeGirl
 * @date : Created in 2019/11/6 16:45
 */
@Controller
@RequestMapping("/extent")
public class ExtentController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExtentController.class);

    @Resource
    public XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobService xxlJobService;

    @RequestMapping(value = "/job_group", method = RequestMethod.GET)
    @PermissionLimit(limit = false)
    @ResponseBody
    public ReturnT<XxlJobGroup> getJobGroup(@RequestParam("appName") String appName,
            @RequestParam("title") String title) {
        // valid
        if (appName == null || appName.trim().length() == 0) {
            return new ReturnT<XxlJobGroup>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (appName.length() < 4 || appName.length() > 64) {
            return new ReturnT<XxlJobGroup>(500, I18nUtil.getString("jobgroup_field_appName_length"));
        }
        if (title == null || title.trim().length() == 0) {
            return new ReturnT<XxlJobGroup>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }

        XxlJobGroup xxlJobGroup = xxlJobGroupDao.findByAppNameAndTitle(appName, title);

        return new ReturnT<XxlJobGroup>(xxlJobGroup);

    }

    @RequestMapping(value = "/job_group/register", method = RequestMethod.POST)
    @PermissionLimit(limit = false)
    @ResponseBody
    public ReturnT<XxlJobGroup> registerJobGroup(XxlJobGroup xxlJobGroup) {
        // valid
        if (xxlJobGroup.getAppName() == null || xxlJobGroup.getAppName().trim().length() == 0) {
            return new ReturnT<XxlJobGroup>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (xxlJobGroup.getAppName().length() < 4 || xxlJobGroup.getAppName().length() > 64) {
            return new ReturnT<XxlJobGroup>(500, I18nUtil.getString("jobgroup_field_appName_length"));
        }
        if (xxlJobGroup.getTitle() == null || xxlJobGroup.getTitle().trim().length() == 0) {
            return new ReturnT<XxlJobGroup>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (xxlJobGroup.getAddressType() != 0) {
            if (xxlJobGroup.getAddressList() == null || xxlJobGroup.getAddressList().trim().length() == 0) {
                return new ReturnT<XxlJobGroup>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            String[] addresss = xxlJobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item == null || item.trim().length() == 0) {
                    return new ReturnT<XxlJobGroup>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }

        XxlJobGroup jobGroup = xxlJobGroupDao.findByAppNameAndTitle(xxlJobGroup.getAppName(), xxlJobGroup.getTitle());
        if (jobGroup != null) {
            LOGGER.info("jobGroup appName[{}] title[{}] is exist. no repeat registration is required.", xxlJobGroup.getAppName(), xxlJobGroup.getTitle());
            return new ReturnT<XxlJobGroup>(jobGroup);
        }

        int ret = xxlJobGroupDao.save(xxlJobGroup);
        if (ret > 0) {
            return new ReturnT<XxlJobGroup>(xxlJobGroup);
        }
        return new ReturnT<XxlJobGroup>(500, "");
    }


    @RequestMapping(value = "/job_info/register", method = RequestMethod.POST)
    @PermissionLimit(limit = false)
    @ResponseBody
    public ReturnT<String> registerJobInfo(XxlJobInfo xxlJobInfo) {
        if (GlueTypeEnum.match(xxlJobInfo.getGlueType()) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(xxlJobInfo.getGlueType())
                && (xxlJobInfo.getExecutorHandler() == null || xxlJobInfo.getExecutorHandler().trim().length() == 0)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
        }
        XxlJobInfo jobInfo = xxlJobInfoDao.findByJobGroupAndExecutorHandler(xxlJobInfo.getJobGroup(), xxlJobInfo.getExecutorHandler());
        if (jobInfo != null) {
            LOGGER.info("jobInfo jobGroupId[{}] ExecutorHandler[{}] is exist. no repeat registration is required.", xxlJobInfo.getJobGroup(), xxlJobInfo.getExecutorHandler());
            // 自动开启
            xxlJobService.start(jobInfo.getId());
            return new ReturnT<String>(String.valueOf(jobInfo.getId()));
        }

        ReturnT<String> result = xxlJobService.add(xxlJobInfo);

        // 自动开启
        xxlJobService.start(Integer.valueOf(result.getContent()));
        return result;

    }

}
