package com.dianping.swallow.web.service;

import java.util.Map;
import java.util.Set;

import com.dianping.swallow.common.server.monitor.data.structure.ConsumerMonitorData;
import com.dianping.swallow.common.server.monitor.data.structure.MonitorData;
import com.dianping.swallow.common.server.monitor.data.structure.ProducerMonitorData;

/**
 * 
 * @author qi.yin
 *
 */
public interface IPCollectorService {

	/**
	 * add all ip
	 * 
	 * @param monitorData
	 */
	public void addIps(MonitorData monitorData);

	/**
	 * add producerServer ip
	 * 
	 * @param monitorData
	 */
	public void addProducerServerIps(ProducerMonitorData producerMonitorData);

	/**
	 * add consumerServer ip
	 * 
	 * @param monitorData
	 */
	public void addConsumerServerIps(ConsumerMonitorData consumerMonitorData);

	/**
	 * get consumerServer ip
	 * 
	 * @param monitorData
	 */
	public Set<String> getConsumerServerIps();

	/**
	 * get producerServer ip
	 * 
	 * @param monitorData
	 */
	public Set<String> getProducerServerIps();

	/**
	 * get all ip
	 * 
	 * @param monitorData
	 */
	public Set<String> getIps();

	/**
	 * get cmdb producers
	 * 
	 * @return
	 */
	public Map<String, String> getCmdbProducers();

	/**
	 * get cmdb consumer slaves
	 * 
	 * @return
	 */
	public Map<String, String> getCmdbConsumerSlaves();

	/**
	 * get cmdb consumer masters
	 * 
	 * @return
	 */
	public Map<String, String> getCmdbConsumerMasters();
	
	/**
	 * clear producerServerIps
	 */
	public void clearProducerServerIps();
	
	/**
	 * clear consumerServerIps
	 */
	public void clearConsumerServerIps();
	
	/**
	 * topic consumerId related ip
	 * @return
	 */
	public Map<String,String> getTopicConsumerIdIps();
	
	/**
	 * producer topic related ip
	 * @return
	 */
	public Map<String,String> getProducerTopicIps();
	
	/**
	 * get one swallow ip
	 * @return
	 */
	public String getServerIp();
	/**
	 * topicConsumerIdIps' key
	 * @param topic
	 * @param consumerId
	 * @return
	 */
	public String getTopicConsumerIdKey(String topic, String consumerId);
	

}