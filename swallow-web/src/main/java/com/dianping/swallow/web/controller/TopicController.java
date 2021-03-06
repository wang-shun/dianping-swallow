package com.dianping.swallow.web.controller;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.swallow.web.common.Pair;
import com.dianping.swallow.web.controller.dto.TopicQueryDto;
import com.dianping.swallow.web.controller.listener.ResourceListener;
import com.dianping.swallow.web.controller.listener.ResourceObserver;
import com.dianping.swallow.web.controller.utils.UserUtils;
import com.dianping.swallow.web.model.resource.BaseResource;
import com.dianping.swallow.web.model.resource.IpInfo;
import com.dianping.swallow.web.model.resource.TopicResource;
import com.dianping.swallow.web.service.TopicResourceService;
import com.dianping.swallow.web.service.UserService;
import com.dianping.swallow.web.util.ResponseStatus;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author mingdongli
 *         <p/>
 *         2015年4月22日 下午1:50:20
 */
@Controller
public class TopicController extends AbstractMenuController implements ResourceObserver {

    private static final String DELIMITOR = ",";

    private static final String POSTFIX = "@dianping.com";

    public static final String DEFAULT = "default";

    @Resource(name = "userService")
    private UserService userService;

    @Resource(name = "topicResourceService")
    private TopicResourceService topicResourceService;

    @Autowired
    private UserUtils userUtils;

    private List<ResourceListener> listeners = new ArrayList<ResourceListener>();

    @RequestMapping(value = "/console/topic")
    public ModelAndView topicView() {

        return new ModelAndView("topic/index", createViewMap());
    }

    @RequestMapping(value = "/console/topic/list", method = RequestMethod.POST)
    @ResponseBody
    public Object fetchTopicPage(@RequestBody TopicQueryDto topicQueryDto, HttpServletRequest request) {

        String topic = topicQueryDto.getTopic();
        String producerIp = topicQueryDto.getProducerServer();
        String administrator = topicQueryDto.getAdministrator();
        boolean inactive = topicQueryDto.isInactive();
        int offset = topicQueryDto.getOffset();
        int limit = topicQueryDto.getLimit();

        boolean isAllEmpty = StringUtil.isAllBlank(topic, producerIp, administrator) && inactive;

        if (isAllEmpty) {
            String username = userUtils.getUsername(request);
            boolean findAll = userUtils.isAdministrator(username);
            if (findAll) {
                return topicResourceService.findTopicResourcePage(offset, limit);
            } else {
                return topicResourceService.findByAdministrator(offset, limit, username);
            }
        } else {
            return topicResourceService.find(offset, limit, topic, producerIp,administrator, inactive);
        }

    }

    @RequestMapping(value = "/console/topic/namelist", method = RequestMethod.GET)
    @ResponseBody
    public Pair<List<String>, List<String>> topicAndIp(HttpServletRequest request) {

        String username = userUtils.getUsername(request);
        List<String> topics = userUtils.topicNames(username);
        List<String> ips = userUtils.producerIps(username);

        return new Pair<List<String>, List<String>>(topics, ips);
    }

    @RequestMapping(value = "/console/topic/update", method = RequestMethod.POST)
    @ResponseBody
    public boolean updateTopic(@RequestBody TopicResource topicResource) {

        boolean result = topicResourceService.update(topicResource);
        if (result) {
            doUpdateNotify(topicResource);
        }
        return result;
    }

    @RequestMapping(value = "/console/topic/auth/ip", method = RequestMethod.POST)
    @ResponseBody
    public Object queryProducerIp(@RequestBody TopicQueryDto topicQueryDto) {

        String topic = topicQueryDto.getTopic();
        return topicResourceService.findByTopic(topic);
    }

    @RequestMapping(value = "/api/topic/edittopic", method = RequestMethod.POST)
    @ResponseBody
    public Object editTopic(@RequestParam(value = "topic") String topic, @RequestParam(value = "prop") String prop,
                            @RequestParam(value = "time") String time, @RequestParam(value = "exec_user") String approver,
                            HttpServletRequest request) {

        String username = userUtils.getUsername(request);
        TopicResource topicResource = null;

        username = StringUtils.isEmpty(username) ? approver : username;

        if (approver != null) {
            if (!userService.loadCachedAdministratorSet().contains(approver)) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format(
                            "%s update topic %s to [prop: %s ], [dept: %s ], [time: %s ] failed. No authentication!",
                            username, topic, prop, splitProps(prop.trim()).toString(), time.toString()));
                }
                return ResponseStatus.UNAUTHENTICATION;
            } else {
                topicResource = topicResourceService.findByTopic(topic);
                if (topicResource == null) {
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format(
                                "%s update topic %s to [prop: %s ], [dept: %s ], [time: %s ] failed. No such topic!",
                                username, topic, prop, splitProps(prop.trim()).toString(), time.toString()));
                    }
                    return ResponseStatus.INVALIDTOPIC;
                }
                String proposal = topicResource.getAdministrator();
                prop = checkProposalName(prop);
                if (StringUtils.isNotEmpty(proposal)) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(proposal).append(DELIMITOR).append(prop);
                    String[] propsals = sb.toString().split(DELIMITOR);
                    Set<String> propsalSet = new HashSet<String>(Arrays.asList(propsals));
                    prop = StringUtils.join(propsalSet, DELIMITOR);
                }
            }
        }

        boolean result = false;

        Transaction producerTransaction = Cat.getProducer().newTransaction("TopicEdit", topic + ":" + username);

        try {
            topicResource.setAdministrator(prop);
            topicResource.setUpdateTime(new Date());
            result = topicResourceService.update(topicResource);
            if (result) {
                doUpdateNotify(topicResource);
            }
            producerTransaction.setStatus(Message.SUCCESS);
        } catch (MongoSocketException e) {
            producerTransaction.setStatus(e);
            Cat.getProducer().logError(e);
        } catch (MongoException e) {
            producerTransaction.setStatus(e);
            Cat.getProducer().logError(e);
        } finally {
            producerTransaction.complete();
        }

        if (result) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("%s update topic %s to [prop: %s ], [dept: %s ], [time: %s ] successfully.",
                        username, topic, prop, splitProps(prop.trim()).toString(), time.toString()));
            }
            return ResponseStatus.SUCCESS;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                        "%s update topic %s to [prop: %s ], [dept: %s ], [time: %s ] failed.Please try again.",
                        username, topic, prop, splitProps(prop.trim()).toString(), time.toString()));
            }
            return ResponseStatus.MONGOWRITE;
        }

    }

    @RequestMapping(value = "/console/topic/producer/alarm", method = RequestMethod.GET)
    @ResponseBody
    public boolean editProducerAlarmSetting(@RequestParam String topic, @RequestParam boolean alarm) {

        TopicResource topicResource = topicResourceService.findByTopic(topic);
        topicResource.setProducerAlarm(alarm);
        boolean result = topicResourceService.update(topicResource);
        if (result) {
            doUpdateNotify(topicResource);
        }
        return result;
    }

    @RequestMapping(value = "/console/topic/consumer/alarm", method = RequestMethod.GET)
    @ResponseBody
    public boolean editConsumerAlarmSetting(@RequestParam String topic, @RequestParam boolean alarm) {

        TopicResource topicResource = topicResourceService.findByTopic(topic);
        topicResource.setConsumerAlarm(alarm);
        boolean result = topicResourceService.update(topicResource);
        if (result) {
            doUpdateNotify(topicResource);
        }
        return result;
    }

    @RequestMapping(value = "/console/topic/administrator", method = RequestMethod.GET)
    @ResponseBody
    public Object loadAdministrators(HttpServletRequest request) {

        String username = userUtils.getUsername(request);
        return userUtils.administrator(username);
    }

    @RequestMapping(value = "/console/topic/alarm/ipinfo/alarm", method = RequestMethod.GET)
    @ResponseBody
    public boolean setAlarm(String topic, String ip, boolean alarm) {

        return doSetIpInfo(topic, ip, "alarm", alarm);
    }

    @RequestMapping(value = "/console/topic/alarm/ipinfo/active", method = RequestMethod.GET)
    @ResponseBody
    public boolean setActive(String topic, String cid, String ip, boolean active) {

        return doSetIpInfo(topic, ip, "active", active);
    }

    private boolean doSetIpInfo(String topic, String ip, String type, boolean value) {

        TopicResource topicResource = topicResourceService.findByTopic(topic);
        List<IpInfo> ipInfos = topicResource.getProducerIpInfos();
        if (ipInfos == null || ip == null || type == null) {
            return false;
        }
        for (IpInfo ipInfo : ipInfos) {
            if (ip.equals(ipInfo.getIp())) {
                if (type.equals("alarm")) {
                    ipInfo.setAlarm(value);
                } else if (type.equals("active")) {
                    ipInfo.setActive(value);
                } else {
                    return false;
                }
                topicResource.setProducerIpInfos(ipInfos);
                boolean result = topicResourceService.insert(topicResource);
                if (result) {
                    doUpdateNotify(topicResource);
                }
                return result;
            }
        }

        return false;
    }

    @RequestMapping(value = "/console/topic/alarm/ipinfo/count/inactive", method = RequestMethod.GET)
    @ResponseBody
    public long countInactive() {

        return topicResourceService.countInactive();
    }

    private String checkProposalName(String proposal) {

        if (proposal.contains(POSTFIX)) {
            int index = proposal.indexOf(POSTFIX);
            proposal = proposal.substring(0, index);
        }
        int index = proposal.indexOf("?");
        if (index != -1) {
            proposal = proposal + "，";
            proposal = proposal.replaceAll("\\?", ",").replaceAll(" ", "").replaceAll("，", ",");
        }
        return proposal;
    }

    private Set<String> splitProps(String props) {
        String[] prop = props.split(DELIMITOR);
        Set<String> lists = new HashSet<String>(Arrays.asList(prop));

        return lists;
    }

    @Override
    protected String getMenu() {
        return "topic";
    }

    @Override
    public void doRegister(ResourceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void doUpdateNotify(BaseResource resource) {
        for (ResourceListener listener : listeners) {
            listener.doUpdateNotify(resource);
        }
    }

    @Override
    public void doDeleteNotify(BaseResource resource) {
        for (ResourceListener listener : listeners) {
            listener.doDeleteNotify(resource);
        }
    }

}