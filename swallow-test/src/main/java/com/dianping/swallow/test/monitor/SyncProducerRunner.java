package com.dianping.swallow.test.monitor;

import java.util.Date;

import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.producer.exceptions.RemoteServiceInitFailedException;
import com.dianping.swallow.producer.Producer;
import com.dianping.swallow.producer.ProducerConfig;
import com.dianping.swallow.producer.ProducerMode;
import com.dianping.swallow.producer.impl.ProducerFactoryImpl;

public class SyncProducerRunner {

	public static void main(String[] args) {

		try {

			ProducerConfig config = new ProducerConfig();
			ProducerMode mode = ProducerMode.SYNC_MODE;
			config.setMode(mode);
			Producer producer = ProducerFactoryImpl.getInstance().createProducer(Destination.topic("LoadTestTopic-0"),
					config);

			for (;;) {

				String msg = new Date() + "message";
				producer.sendMessage(msg);
				Thread.sleep(20);
				System.out.println("*****************");
			}
		} catch (Exception e) {
		}
	}
}