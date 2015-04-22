package com.dianping.swallow.web.dao.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.dianping.swallow.common.server.monitor.data.ProducerMonitorData;
import com.dianping.swallow.web.dao.ProducerMonitorDao;

/**
 * @author mengwenchao
 *
 * 2015年4月17日 下午4:09:14
 */
@Component
public class DefaultProducerMonitorDao extends AbstractMonitorDao implements ProducerMonitorDao{
	
	@Override
	public void saveProducerMonotorData(ProducerMonitorData producerMonitorData) {
		
		mongoTemplate.save(producerMonitorData);
	}

	@Override
	public void saveProducerMonotorData(List<ProducerMonitorData> producerMonitorDatas) {
		
		for(ProducerMonitorData producerMonitorData : producerMonitorDatas){
			saveProducerMonotorData(producerMonitorData);
			
		}
		
	}

}
