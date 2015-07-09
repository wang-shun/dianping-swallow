package com.dianping.swallow.web.alarmer.impl;

import com.dianping.swallow.web.monitor.impl.AbstractRetriever;

/**
 *
 * @author qiyin
 *
 */
public abstract class AbstractStatisAlarmer extends AbstractAlarmer {

	protected static final long TIME_SECTION = 5 * 60 / 5;

	protected long getPreDayKey(long timeKey) {
		return timeKey - AbstractRetriever.getKey(24 * 60 * 60 * 1000).longValue();
	}
	
	
	protected static final long getTimeSection(){
		return TIME_SECTION;
	}
	
}
