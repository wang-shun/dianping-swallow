package com.dianping.swallow.web.controller;

import com.dianping.lion.client.ConfigCache;
import com.dianping.lion.client.ConfigChange;
import com.dianping.lion.client.LionException;
import com.dianping.swallow.common.internal.action.SwallowCallableWrapper;
import com.dianping.swallow.common.internal.action.impl.CatCallableWrapper;
import com.dianping.swallow.common.server.monitor.data.QPX;
import com.dianping.swallow.common.server.monitor.data.structure.MonitorData;
import com.dianping.swallow.web.dashboard.model.ResultEntry;
import com.dianping.swallow.web.monitor.*;
import com.dianping.swallow.web.monitor.ConsumerDataRetriever.ConsumerDataPair;
import com.dianping.swallow.web.monitor.charts.ChartBuilder;
import com.dianping.swallow.web.monitor.charts.HighChartsWrapper;
import com.dianping.swallow.web.monitor.collector.MongoStatsDataCollector;
import com.dianping.swallow.web.service.MinuteEntryService;
import com.dianping.swallow.web.service.impl.AbstractServerReportService;
import com.dianping.swallow.web.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * @author mengwenchao
 *         <p/>
 *         2015年4月14日 下午9:24:38
 */
@Controller
public class DataMonitorController extends AbstractMonitorController implements InitializingBean {

    public static final String Y_AXIS_TYPE_MESSAGE = "消息数";

    public static final String Y_AXIS_TYPE_QPS = "QPS";

    public static final String Y_AXIS_TYPE_DELAY = "延时(毫秒)";

    public static final String Y_AXIS_TYPE_ACCUMULATION = "堆积消息数";

    public static final String CAT_TYPE = "MONITOR";

    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final String STATSDATA_QUERY_TIMESPAN_KEY = "swallow.web.statsdata.query.timespan";

    private static final long TIMESPAN_UNIT = 60 * 60 * 1000;

    private static final long maxOrderTimeSpan = 3 * TIMESPAN_UNIT;

    private volatile Integer queryTimeSpan;

    @Value("${swallow.web.monitor.report.maxtimespan}")
    public int reportSpan = 365;

    @Autowired
    private ProducerDataRetriever producerDataRetriever;

    @Autowired
    private ConsumerDataRetriever consumerDataRetriever;

    @Autowired
    private AccumulationRetriever accumulationRetriever;

    @Autowired
    private MongoDataRetriever mongoDataRetriever;

    @Autowired
    private DailyReportRetriever dailyReportRetriever;

    ConfigCache configCache;

    @Resource(name = "minuteEntryService")
    private MinuteEntryService minuteEntryService;

    private void initLionConfig() {
        try {
            configCache = ConfigCache.getInstance();
            queryTimeSpan = configCache.getIntProperty(STATSDATA_QUERY_TIMESPAN_KEY);
            configCache.addChange(new ConfigChange() {
                @Override
                public void onChange(String key, String value) {
                    if (STATSDATA_QUERY_TIMESPAN_KEY.equals(key)) {
                        if (StringUtils.isNotBlank(value)) {
                            queryTimeSpan = Integer.valueOf(value);
                        }
                    }
                }
            });
        } catch (LionException e) {
            logger.error("lion read producer and consumer server ips failed", e);
        }
    }

    @RequestMapping(value = "/console/monitor/consumerserver/qps", method = RequestMethod.GET)
    public ModelAndView viewConsumerServerQps() {

        return new ModelAndView("monitor/consumerserverqps", createViewMap("server", "consumerserverqps"));
    }

    @RequestMapping(value = "/console/monitor/producerserver/qps", method = RequestMethod.GET)
    public ModelAndView viewProducerServerQps() {

        return new ModelAndView("monitor/producerserverqps", createViewMap("server", "producerserverqps"));
    }

    @RequestMapping(value = "/console/monitor/mongo/qps", method = RequestMethod.GET)
    public ModelAndView viewMongoServerQps() {

        return new ModelAndView("monitor/mongoqps", createViewMap("server", "mongoqps"));
    }

    @RequestMapping(value = "/console/monitor/report/producer", method = RequestMethod.GET)
    public ModelAndView viewPServerReport() {

        return new ModelAndView("monitor/producerreport", createViewMap("report", "producerreport"));
    }

    @RequestMapping(value = "/console/monitor/report/consumer", method = RequestMethod.GET)
    public ModelAndView viewCServerReport() {

        return new ModelAndView("monitor/consumerreport", createViewMap("report", "consumerreport"));
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/qps", method = RequestMethod.GET)
    public ModelAndView viewTopicQps(@PathVariable String topic) {

        return new ModelAndView("monitor/consumerqps", createViewMap("topic", "consumerqps"));
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/ip/qps", method = RequestMethod.GET)
    public ModelAndView viewConsumerIpQps(@PathVariable String topic) {

        return new ModelAndView("monitor/consumeripqps", createViewMap("topic", "consumerqps"));
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/ip/delay", method = RequestMethod.GET)
    public ModelAndView viewConsumerIpDelay(@PathVariable String topic) {

        return new ModelAndView("monitor/consumeripdelay", createViewMap("topic", "delay"));
    }

    @RequestMapping(value = "/console/monitor/producer/{topic}/ip/qps", method = RequestMethod.GET)
    public ModelAndView viewProducerIpQps(@PathVariable String topic) {

        return new ModelAndView("monitor/produceripqps", createViewMap("topic", "consumerqps"));
    }

    @RequestMapping(value = "/console/monitor/producer/{topic}/ip/delay", method = RequestMethod.GET)
    public ModelAndView viewProducerIpDelay(@PathVariable String topic) {

        return new ModelAndView("monitor/produceripdelay", createViewMap("topic", "delay"));
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/accu", method = RequestMethod.GET)
    public ModelAndView viewTopicAccumulation(@PathVariable String topic, HttpServletRequest request) {
        if (topic.equals(MonitorData.TOTAL_KEY)) {
            String firstTopic = getFirstTopic(accumulationRetriever.getTopics());
            if (!firstTopic.equals(MonitorData.TOTAL_KEY)) {
                return new ModelAndView("redirect:/console/monitor/consumer/" + firstTopic + "/accu", createViewMap(
                        "topic", "consumeraccu"));
            }
        }
        return new ModelAndView("monitor/consumeraccu", createViewMap("topic", "consumeraccu"));
    }

    @RequestMapping(value = "/console/monitor/dashboard", method = RequestMethod.GET)
    public ModelAndView viewTopicdashboarddelay() {

        return new ModelAndView("monitor/consumerdashboarddelay", createViewMap("dashboard", "dashboarddelay"));
    }

    @RequestMapping(value = "/console/monitor/dashboard/delay/minute", method = RequestMethod.GET)
    @ResponseBody
    public Object getConsumerIdDelayDashboard(@RequestParam("date") String date, @RequestParam("step") int step,
                                              @RequestParam("type") String type) throws Exception {

        Date stop = adjustTimeByStep(date, step);

        Date start = calStartTime(stop);

        List<ResultEntry> entrys = minuteEntryService.loadMinuteEntryPage(start, stop, type);

        return buildResponse(entrys, type.trim());

    }

    private String getFirstTopic(Set<String> topics) {

        if (topics == null || topics.size() == 0) {

            return MonitorData.TOTAL_KEY;
        }
        return topics.toArray(new String[0])[0];
    }

    @RequestMapping(value = "/console/monitor/producer/{topic}/savedelay", method = RequestMethod.GET)
    public ModelAndView viewProducerDelayMonitor(@PathVariable String topic) throws IOException {

        Map<String, Object> map = createViewMap("topic", "delay");
        return new ModelAndView("monitor/producerdelay", map);

    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/delay", method = RequestMethod.GET)
    public ModelAndView viewConsumerDelayMonitor(@PathVariable String topic) throws IOException {

        Map<String, Object> map = createViewMap("topic", "delay");
        return new ModelAndView("monitor/consumerdelay", map);
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/order", method = RequestMethod.GET)
    public ModelAndView viewConsumerOrder(@PathVariable String topic) {

        return new ModelAndView("monitor/consumerorder", createViewMap("topic", "consumerorder"));
    }

    @RequestMapping(value = "/console/monitor/mongo/debug/{server}", method = RequestMethod.GET)
    @ResponseBody
    public String getMongoDebug(@PathVariable String server) {

        if (logger.isInfoEnabled()) {
            logger.info("[getMongoDebug]" + server);
            logger.info(mongoDataRetriever.getMongoDebugInfo(server));
        }
        return "ok";
    }

    @RequestMapping(value = "/console/monitor/consumer/debug/{server}", method = RequestMethod.GET)
    @ResponseBody
    public String getConsumerDebug(@PathVariable String server) {

        if (logger.isInfoEnabled()) {
            logger.info("[getConsumerDebug]" + server);
            logger.info(consumerDataRetriever.getDebugInfo(server));
        }
        return "ok";
    }

    @RequestMapping(value = "/console/monitor/producer/debug/{server}", method = RequestMethod.GET)
    @ResponseBody
    public String getProducerDebug(@PathVariable String server) {

        if (logger.isInfoEnabled()) {
            logger.info("[getProducerDebug]" + server);
            logger.info(producerDataRetriever.getDebugInfo(server));
        }
        return "ok";
    }

    @RequestMapping(value = "/console/monitor/consumerserver/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerServerQps() {

        Map<String, ConsumerDataPair> serverQpx = consumerDataRetriever.getServerQpx(QPX.SECOND);

        return buildConsumerHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/consumerserver/qps/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerServerQps(@PathVariable String startTime, @PathVariable String endTime) {
        Map<String, ConsumerDataPair> serverQpx = null;
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getConsumerServerQps();
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);

        serverQpx = consumerDataRetriever.getServerQpx(QPX.SECOND, searchTime.getStartTime(), searchTime.getEndTime());
        return buildConsumerHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/producerserver/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getProducerServerQps() {

        Map<String, StatsData> serverQpx = producerDataRetriever.getServerQpx(QPX.SECOND);

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/producerserver/qps/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getProducerServerQps(@PathVariable String startTime, @PathVariable String endTime) {
        Map<String, StatsData> serverQpx = null;
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getProducerServerQps();
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);
        serverQpx = producerDataRetriever.getServerQpx(QPX.SECOND, searchTime.getStartTime(), searchTime.getEndTime());

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/mongo/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getMongoQps() {

        Map<MongoStatsDataCollector.MongoStatsDataKey, StatsData> serverQpx = mongoDataRetriever.getMongoQpx(QPX.SECOND);

        return buildMongoHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/mongo/qps/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getMongoQps(@PathVariable String startTime, @PathVariable String endTime) {
        Map<MongoStatsDataCollector.MongoStatsDataKey, StatsData> serverQpx = null;
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getMongoQps();
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);
        serverQpx = mongoDataRetriever.getMongoQpx(QPX.SECOND, searchTime.getStartTime(), searchTime.getEndTime());

        return buildMongoHighChartsWrapper(Y_AXIS_TYPE_QPS, serverQpx);
    }

    @RequestMapping(value = "/console/monitor/report/producer/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getProducerServerReport() {

        Map<String, StatsData> serverReport = dailyReportRetriever.getProducerServerMessageCount();
        serverReport = sortedStatsData(serverReport);

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_MESSAGE, serverReport);
    }

    @RequestMapping(value = "/console/monitor/report/producer/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getProducerServerReport(@PathVariable String startTime, @PathVariable String endTime) {
        Map<String, StatsData> serverReport;
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getProducerServerReport();
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, reportSpan * TIMESPAN_UNIT * 24);
        serverReport = dailyReportRetriever.getProducerServerMessageCount(searchTime.getStartTime(), searchTime.getEndTime());
        serverReport = sortedStatsData(serverReport);

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_MESSAGE, serverReport);
    }

    @RequestMapping(value = "/console/monitor/report/consumer/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerServerReport() {

        Map<String, StatsData> serverReport = dailyReportRetriever.getConsumerServerMessageCount();
        serverReport = sortedStatsData(serverReport);

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_MESSAGE, serverReport);
    }

    @RequestMapping(value = "/console/monitor/report/consumer/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerServerReport(@PathVariable String startTime, @PathVariable String endTime) {
        Map<String, StatsData> serverReport;
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getConsumerServerReport();
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, reportSpan * TIMESPAN_UNIT * 24);
        serverReport = dailyReportRetriever.getConsumerServerMessageCount(searchTime.getStartTime(), searchTime.getEndTime());
        serverReport = sortedStatsData(serverReport);

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_MESSAGE, serverReport);
    }

    @RequestMapping(value = "/console/monitor/producer/{topic}/ip/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicIpQps(@PathVariable String topic) {
        List<IpStatsData> statsDatas = producerDataRetriever.getAllIpQpxList(topic);

        return buildProducerHighChartsWrapper(topic, Y_AXIS_TYPE_QPS, statsDatas);
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/ip/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerIdIpQpx(@PathVariable final String topic,
                                                      @RequestParam(value = "cid", required = true) String consumerId) throws Exception {
        List<IpStatsData> consumerQpx = consumerDataRetriever.getAllIpQpxList(topic, consumerId);

        return buildConsumerHighChartsWrapper(consumerId, Y_AXIS_TYPE_QPS, consumerQpx);

    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/qps/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicQps(@PathVariable String topic,
                                               @RequestParam(value = "cid", required = false) String consumerIds) {

        Set<String> interestConsumerIds = getConsumerIds(consumerIds);
        StatsData producerData = producerDataRetriever.getQpx(topic, QPX.SECOND);
        List<ConsumerDataPair> consumerData = consumerDataRetriever.getQpxForAllConsumerId(topic, QPX.SECOND);

        return buildConsumerHighChartsWrapper(topic, Y_AXIS_TYPE_QPS, producerData, consumerData, interestConsumerIds);
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/qps/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicQps(@PathVariable String topic,
                                               @RequestParam(value = "cid", required = false) String consumerIds, @PathVariable String startTime,
                                               @PathVariable String endTime) {

        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getTopicQps(topic, consumerIds);
        }

        Set<String> interestConsumerIds = getConsumerIds(consumerIds);

        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);
        StatsData producerData = producerDataRetriever.getQpx(topic, QPX.SECOND, searchTime.getStartTime(),
                searchTime.getEndTime());
        List<ConsumerDataPair> consumerData = consumerDataRetriever.getQpxForAllConsumerId(topic, QPX.SECOND,
                searchTime.getStartTime(), searchTime.getEndTime());

        return buildConsumerHighChartsWrapper(topic, Y_AXIS_TYPE_QPS, producerData, consumerData, interestConsumerIds);
    }

    private Map<String, StatsData> sortedStatsData(Map<String, StatsData> serverReport){
        Map<String, StatsData> linkedServerReport = new LinkedHashMap<String, StatsData>();
        if(serverReport.get(AbstractServerReportService.TOTAL) != null){
            Set<String> keySet = serverReport.keySet();
            linkedServerReport.put(AbstractServerReportService.TOTAL, serverReport.get(AbstractServerReportService.TOTAL));
            for(String ip: keySet){
                if(!AbstractServerReportService.TOTAL.equalsIgnoreCase(ip)){
                    linkedServerReport.put(ip, serverReport.get(ip));
                }
            }
            return linkedServerReport;
        }
        return serverReport;
    }

    private Set<String> getConsumerIds(String consumerIds) {

        if (consumerIds == null) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        String[] split = consumerIds.split("\\s*,\\s*");
        for (String consumerId : split) {

            if (StringUtils.isEmpty(consumerId)) {
                continue;
            }
            result.add(consumerId.trim());
        }

        if (logger.isInfoEnabled()) {
            logger.info("[getConsumerIds]" + result);
        }
        return result;
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/accu/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicAccumulation(@PathVariable String topic,
                                                        @RequestParam(value = "cid", required = false) String consumerIds) {

        Set<String> interestConsumerIds = getConsumerIds(consumerIds);

        Map<String, StatsData> statsData = accumulationRetriever.getAccumulationForAllConsumerId(topic);

        if (interestConsumerIds != null) {

            List<String> remove = new LinkedList<String>();
            for (String consumerId : statsData.keySet()) {
                if (!interestConsumerIds.contains(consumerId)) {
                    remove.add(consumerId);
                }
            }

            for (String consumerId : remove) {
                statsData.remove(consumerId);
            }
        }

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_ACCUMULATION, statsData);
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/accu/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicAccumulation(@PathVariable String topic,
                                                        @RequestParam(value = "cid", required = false) String consumerIds, @PathVariable String startTime,
                                                        @PathVariable String endTime) {

        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getTopicAccumulation(topic, consumerIds);
        }

        Set<String> interestConsumerIds = getConsumerIds(consumerIds);

        final SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);

        Map<String, StatsData> statsData = accumulationRetriever.getAccumulationForAllConsumerId(topic,
                searchTime.getStartTime(), searchTime.getEndTime());

        if (interestConsumerIds != null) {

            List<String> remove = new LinkedList<String>();
            for (String consumerId : statsData.keySet()) {
                if (!interestConsumerIds.contains(consumerId)) {
                    remove.add(consumerId);
                }
            }

            for (String consumerId : remove) {
                statsData.remove(consumerId);
            }
        }

        return buildStatsHighChartsWrapper(Y_AXIS_TYPE_ACCUMULATION, statsData);
    }

    @RequestMapping(value = "/console/monitor/topiclist/get", method = RequestMethod.POST)
    @ResponseBody
    public Set<String> getProducerDelayMonitor() throws IOException {
        return allTopics();
    }

    private Set<String> allTopics() {

        Set<String> producerTopics = producerDataRetriever.getTopics();
        Set<String> consumerTopics = consumerDataRetriever.getTopics();
        producerTopics.addAll(consumerTopics);
        return producerTopics;
    }

    @RequestMapping(value = "/console/monitor/producer/{topic}/ip/delay/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getTopicIpDelay(@PathVariable String topic) {
        List<IpStatsData> statsDatas = producerDataRetriever.getAllIpDelayList(topic);

        return buildProducerHighChartsWrapper(topic, Y_AXIS_TYPE_DELAY, statsDatas);
    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/ip/delay/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerIdIpDelay(@PathVariable final String topic,
                                                        @RequestParam(value = "cid", required = true) String consumerId) throws Exception {
        List<IpStatsData> consumerDelay = consumerDataRetriever.getAllIpDelayList(topic, consumerId);

        return buildConsumerHighChartsWrapper(consumerId, Y_AXIS_TYPE_DELAY, consumerDelay);

    }


    @RequestMapping(value = "/console/monitor/consumer/{topic}/delay/get", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerDelayMonitor(@PathVariable final String topic,
                                                           @RequestParam(value = "cid", required = false) String consumerIds) throws Exception {

        final Set<String> interestConsumerIds = getConsumerIds(consumerIds);

        SwallowCallableWrapper<List<HighChartsWrapper>> wrapper = new CatCallableWrapper<List<HighChartsWrapper>>(
                CAT_TYPE, "getConsumerDelayMonitor");

        return wrapper.doCallable(new Callable<List<HighChartsWrapper>>() {

            @Override
            public List<HighChartsWrapper> call() throws Exception {

                StatsData producerData = producerDataRetriever.getSaveDelay(topic);
                List<ConsumerDataPair> consumerDelay = consumerDataRetriever.getDelayForAllConsumerId(topic);

                return buildConsumerHighChartsWrapper(topic, Y_AXIS_TYPE_DELAY, producerData, consumerDelay,
                        interestConsumerIds);
            }
        });

    }

    @RequestMapping(value = "/console/monitor/consumer/{topic}/delay/get/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public List<HighChartsWrapper> getConsumerDelayMonitor(@PathVariable final String topic,
                                                           @RequestParam(value = "cid", required = false) String consumerIds, @PathVariable String startTime,
                                                           @PathVariable String endTime) throws Exception {

        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getConsumerDelayMonitor(topic, consumerIds);
        }

        final Set<String> interestConsumerIds = getConsumerIds(consumerIds);

        final SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, true, getQueryTimeSpan()
                * TIMESPAN_UNIT);

        SwallowCallableWrapper<List<HighChartsWrapper>> wrapper = new CatCallableWrapper<List<HighChartsWrapper>>(
                CAT_TYPE, "getConsumerDelayMonitor");

        return wrapper.doCallable(new Callable<List<HighChartsWrapper>>() {

            @Override
            public List<HighChartsWrapper> call() throws Exception {

                StatsData producerData = producerDataRetriever.getSaveDelay(topic, searchTime.getStartTime(),
                        searchTime.getEndTime());
                List<ConsumerDataPair> consumerDelay = consumerDataRetriever.getDelayForAllConsumerId(topic,
                        searchTime.getStartTime(), searchTime.getEndTime());

                return buildConsumerHighChartsWrapper(topic, Y_AXIS_TYPE_DELAY, producerData, consumerDelay,
                        interestConsumerIds);
            }
        });

    }

    @RequestMapping(value = "/console/monitor/consumer/total/order/get/{size}", method = RequestMethod.POST)
    @ResponseBody
    public Object getConsumerOrderMonitor(@PathVariable final int size) throws Exception {
        List<OrderStatsData> pOrderStatsDatas = producerDataRetriever.getOrder(size);
        List<OrderStatsData> cOrderStatsDatas = consumerDataRetriever.getOrderForAllConsumerId(size);
        List<OrderStatsData> orderStatsDatas = new ArrayList<OrderStatsData>();
        orderStatsDatas.addAll(pOrderStatsDatas);
        orderStatsDatas.addAll(cOrderStatsDatas);
        return orderStatsDatas;
    }

    @RequestMapping(value = "/console/monitor/consumer/total/order/get/{size}/{startTime}/{endTime}", method = RequestMethod.POST)
    @ResponseBody
    public Object getConsumerOrderMonitor(@PathVariable final int size, @PathVariable String startTime,
                                          @PathVariable String endTime) throws Exception {
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return getConsumerOrderMonitor(size);
        }
        SearchTime searchTime = new SearchTime().getSearchTime(startTime, endTime, false, maxOrderTimeSpan);
        List<OrderStatsData> pOrderStatsDatas = producerDataRetriever.getOrder(size, searchTime.getStartTime(),
                searchTime.getEndTime());
        List<OrderStatsData> cOrderStatsDatas = consumerDataRetriever.getOrderForAllConsumerId(size,
                searchTime.getStartTime(), searchTime.getEndTime());
        List<OrderStatsData> orderStatsDatas = new ArrayList<OrderStatsData>();
        orderStatsDatas.addAll(pOrderStatsDatas);
        orderStatsDatas.addAll(cOrderStatsDatas);
        return orderStatsDatas;
    }

    private List<HighChartsWrapper> buildStatsHighChartsWrapper(String yAxis, Map<String, StatsData> stats) {

        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(stats.size());
        for (Entry<String, StatsData> entry : stats.entrySet()) {

            String key = entry.getKey();
            StatsData statsData = entry.getValue();

            result.add(ChartBuilder.getHighChart(key, "", yAxis, statsData));

        }

        return result;
    }

    private List<HighChartsWrapper> buildConsumerHighChartsWrapper(String yAxis, Map<String, ConsumerDataPair> serverQpx) {

        int size = serverQpx.size();
        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(size);

        for (Entry<String, ConsumerDataPair> entry : serverQpx.entrySet()) {

            String ip = entry.getKey();
            ConsumerDataPair dataPair = entry.getValue();
            List<StatsData> allStats = new LinkedList<StatsData>();
            if (isEmpty(dataPair.getSendData()) && isEmpty(dataPair.getAckData())) {
                continue;
            }
            allStats.add(dataPair.getSendData());
            allStats.add(dataPair.getAckData());
            result.add(ChartBuilder.getHighChart(ip, "", yAxis, allStats));
        }

        return result;
    }

    private List<HighChartsWrapper> buildMongoHighChartsWrapper(String yAxis, Map<MongoStatsDataCollector.MongoStatsDataKey, StatsData> stats) {

        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(stats.size());
        for (Entry<MongoStatsDataCollector.MongoStatsDataKey, StatsData> entry : stats.entrySet()) {

            MongoStatsDataCollector.MongoStatsDataKey key = entry.getKey();
            String title = key.getCatalog();
            String subTitle = key.getIp();
            StatsData statsData = entry.getValue();

            result.add(ChartBuilder.getHighChart(title, subTitle, yAxis, statsData));

        }

        return result;
    }

    private boolean isEmpty(StatsData sendData) {

        return sendData == null || sendData.getArrayData() == null || sendData.getArrayData().length == 0;
    }

    private List<HighChartsWrapper> buildConsumerHighChartsWrapper(String topic, String yAxis, StatsData producerData,
                                                                   List<ConsumerDataPair> consumerData, Set<String> interestConsumerIds) {

        int size = consumerData.size();
        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(size);

        for (int i = 0; i < consumerData.size(); i++) {

            ConsumerDataPair dataPair = consumerData.get(i);
            String currentConsumerId = dataPair.getConsumerId();
            if (interestConsumerIds != null && !interestConsumerIds.contains(currentConsumerId)) {
                continue;
            }

            List<StatsData> allStats = new LinkedList<StatsData>();
            allStats.add(producerData);
            allStats.add(dataPair.getSendData());
            allStats.add(dataPair.getAckData());
            result.add(ChartBuilder.getHighChart(getTopicDesc(topic, yAxis), getConsumerIdDesc(topic, currentConsumerId, yAxis), yAxis,
                    allStats));
        }

        if (result == null || result.isEmpty()) {
            result.add(ChartBuilder.getHighChart(getTopicDesc(topic, yAxis), "", yAxis, producerData));
        }

        return result;
    }

    private List<HighChartsWrapper> buildProducerHighChartsWrapper(String topic, String yAxis, List<IpStatsData> statsDatas) {
        if (statsDatas == null) {
            return new ArrayList<HighChartsWrapper>();
        }
        int size = statsDatas.size();
        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(size);

        for (IpStatsData ipStatsData : statsDatas) {
            String appName = ipStatsData.getAppName();
            String ip = ipStatsData.getIp();
            ProducerStatsData statsData = (ProducerStatsData) ipStatsData.getStatsData();

            result.add(ChartBuilder.getHighChart(topic, getIpDesc(appName, ip), yAxis, statsData.getStatsData()));
        }

        return result;
    }

    private List<HighChartsWrapper> buildConsumerHighChartsWrapper(String consumerId, String yAxis, List<IpStatsData> statsDatas) {
        if (statsDatas == null) {
            return new ArrayList<HighChartsWrapper>();
        }
        int size = statsDatas.size();
        List<HighChartsWrapper> result = new ArrayList<HighChartsWrapper>(size);

        for (IpStatsData ipStatsData : statsDatas) {
            String appName = ipStatsData.getAppName();
            String ip = ipStatsData.getIp();
            ConsumerStatsData statsData = (ConsumerStatsData) ipStatsData.getStatsData();
            List<StatsData> allStats = new LinkedList<StatsData>();
            if (isEmpty(statsData.getSendData()) && isEmpty(statsData.getAckData())) {
                continue;
            }
            allStats.add(statsData.getSendData());
            allStats.add(statsData.getAckData());

            result.add(ChartBuilder.getHighChart(consumerId, getIpDesc(appName, ip), yAxis, allStats));
        }

        return result;
    }

    private String getIpDesc(String appName, String ip) {
        String showName = ip;
        if (StringUtils.isNotEmpty(appName)) {
            showName = appName + " " + ip;
        }
        return showName;
    }

    private String getConsumerIdDesc(String topic, String consumerId, String yAxis) {

        if (consumerId.equals(MonitorData.TOTAL_KEY)) {
            return "所有consumerId";
        }
        if (StringUtils.isEmpty(consumerId)) {
            return StringUtils.EMPTY;
        }
        String result = consumerId;
        if (Y_AXIS_TYPE_QPS.equals(yAxis)) {
            result = "<a href=\"/console/monitor/consumer/" + topic + "/ip/qps?cid=" + consumerId + "\">" + consumerId + "</a>";
        } else {
            result = "<a href=\"/console/monitor/consumer/" + topic + "/ip/delay?cid=" + consumerId + "\">" + consumerId + "</a>";
        }

        return result;
    }

    private String getTopicDesc(String topic, String yAxis) {

        if (topic.equals(MonitorData.TOTAL_KEY)) {
            return "所有topic";
        }
        String result = topic;
        if (Y_AXIS_TYPE_QPS.equals(yAxis)) {
            result = "<a href=\"/console/monitor/producer/" + topic + "/ip/qps\">" + topic + "</a>";
        } else {
            result = "<a href=\"/console/monitor/producer/" + topic + "/ip/delay\">" + topic + "</a>";
        }
        return result;
    }

    private Object buildResponse(List<ResultEntry> entrys, String type) {

        Map<String, Object> result = new HashMap<String, Object>();

        if (!entrys.isEmpty()) {
            Date first = entrys.get(0).getTime();
            result = addTime(first);

            result.put("entry", entrys);

            return result;
        }

        return addTime(new Date());
    }

    private Map<String, Object> addTime(Date first) {

        Map<String, Object> result = new HashMap<String, Object>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(first);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        Date starttime = calendar.getTime();

        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        Date stoptime = calendar.getTime();

        result.put("starttime", starttime);
        result.put("stoptime", stoptime);

        return result;
    }

    private Date adjustTimeByStep(String date, int step) throws Exception {

        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT);
        String transferDate = date.replaceAll("Z", "+0800").replaceAll("\"", "");
        Date newdate = formatter.parse(transferDate);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newdate);
        calendar.add(Calendar.HOUR_OF_DAY, step);

        return calendar.getTime();
    }

    private Date calStartTime(Date stop) {

        Calendar calendarstart = Calendar.getInstance();
        calendarstart.setTime(stop);
        calendarstart.add(Calendar.MINUTE, -11);
        calendarstart.clear(Calendar.SECOND);
        calendarstart.clear(Calendar.MILLISECOND);
        Date start = calendarstart.getTime();

        return start;
    }

    @Override
    public String getSide() {
        return "delay";
    }

    private String subSide = "delay";

    @Override
    public String getSubSide() {
        return subSide;
    }

    private static class SearchTime {

        private long startTime;

        private long endTime;

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public SearchTime getSearchTime(String startTime, String endTime, boolean isLimited, long maxTimeSpan) {

            if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {

                this.setStartTime(DateUtil.convertStrToDate(startTime).getTime());
                this.setEndTime(DateUtil.convertStrToDate(endTime).getTime());
                if (isLimited) {
                    if (this.getEndTime() - this.getStartTime() > maxTimeSpan) {
                        this.setEndTime(this.getStartTime() + maxTimeSpan);
                    }
                }

            } else {
                if (StringUtils.isBlank(startTime) && StringUtils.isNotBlank(endTime)) {
                    this.setEndTime(DateUtil.convertStrToDate(endTime).getTime());
                    this.setStartTime(this.getEndTime() - maxTimeSpan);
                } else if (StringUtils.isNotBlank(startTime) && StringUtils.isBlank(endTime)) {
                    this.setStartTime(DateUtil.convertStrToDate(startTime).getTime());
                    if (isLimited) {
                        this.setEndTime(this.getStartTime() + maxTimeSpan);
                    } else {
                        this.setEndTime(System.currentTimeMillis());
                    }
                }
            }
            return this;
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initLionConfig();
    }

    public int getQueryTimeSpan() {
        if (queryTimeSpan != null) {
            return queryTimeSpan.intValue();
        }
        return 3;
    }

}
