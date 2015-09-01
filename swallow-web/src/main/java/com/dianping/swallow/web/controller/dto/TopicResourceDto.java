package com.dianping.swallow.web.controller.dto;


public class TopicResourceDto {
	
	private String id;

	private String topic;
	
	private String administrator;
	
	private boolean producerAlarm;

	private boolean consumerAlarm;
	
	private String producerServer;

	private long sendpeak;
	
	private long sendvalley;
	
	private int sendfluctuation;
	
	private long sendfluctuationBase;
	
	private long delay;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getAdministrator() {
		return administrator;
	}

	public void setAdministrator(String administrator) {
		this.administrator = administrator;
	}

	public boolean isProducerAlarm() {
		return producerAlarm;
	}

	public void setProducerAlarm(boolean producerAlarm) {
		this.producerAlarm = producerAlarm;
	}

	public boolean isConsumerAlarm() {
		return consumerAlarm;
	}

	public void setConsumerAlarm(boolean consumerAlarm) {
		this.consumerAlarm = consumerAlarm;
	}

	public String getProducerServer() {
		return producerServer;
	}

	public void setProducerServer(String producerServer) {
		this.producerServer = producerServer;
	}

	public long getSendpeak() {
		return sendpeak;
	}

	public void setSendpeak(long sendpeak) {
		this.sendpeak = sendpeak;
	}

	public long getSendvalley() {
		return sendvalley;
	}

	public void setSendvalley(long sendvalley) {
		this.sendvalley = sendvalley;
	}

	public int getSendfluctuation() {
		return sendfluctuation;
	}

	public void setSendfluctuation(int sendfluctuation) {
		this.sendfluctuation = sendfluctuation;
	}

	public long getSendfluctuationBase() {
		return sendfluctuationBase;
	}

	public void setSendfluctuationBase(long sendfluctuationBase) {
		this.sendfluctuationBase = sendfluctuationBase;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}
	
}