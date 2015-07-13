package com.dianping.swallow.web.model.statis;

/**
 * 
 * @author qiyin
 *
 */
public class TopicStatsData {
	
	private String id;
	
	private long timeKey;

	private String topicName;

	private ProducerBaseStatsData producerStatisData;
	
	private ConsumerBaseStatsData consumerStatisData;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTimeKey() {
		return timeKey;
	}

	public void setTimeKey(long timeKey) {
		this.timeKey = timeKey;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public ProducerBaseStatsData getProducerStatisData() {
		return producerStatisData;
	}

	public void setProducerStatisData(ProducerBaseStatsData producerStatisData) {
		this.producerStatisData = producerStatisData;
	}

	public ConsumerBaseStatsData getConsumerStatisData() {
		return consumerStatisData;
	}

	public void setConsumerStatisData(ConsumerBaseStatsData consumerStatisData) {
		this.consumerStatisData = consumerStatisData;
	}

}
