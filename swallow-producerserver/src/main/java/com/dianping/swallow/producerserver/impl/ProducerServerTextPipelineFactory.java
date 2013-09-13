package com.dianping.swallow.producerserver.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

import com.dianping.swallow.common.internal.codec.JsonDecoder;
import com.dianping.swallow.common.internal.codec.JsonEncoder;
import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.whitelist.TopicWhiteList;

public class ProducerServerTextPipelineFactory implements ChannelPipelineFactory {
   private MessageDAO     messageDAO;
   private TopicWhiteList topicWhiteList;

   public ProducerServerTextPipelineFactory(MessageDAO messageDAO, TopicWhiteList topicWhiteList) {
      this.messageDAO = messageDAO;
      this.topicWhiteList = topicWhiteList;
   }

   @Override
   public ChannelPipeline getPipeline() {
      ChannelPipeline pipeline = Channels.pipeline();

      pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
      pipeline.addLast("jsonDecoder", new JsonDecoder(TextObject.class));

      pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
      pipeline.addLast("jsonEncoder", new JsonEncoder(TextACK.class));

      pipeline.addLast("handler", new ProducerServerTextHandler(messageDAO, topicWhiteList));

      return pipeline;
   }
}
