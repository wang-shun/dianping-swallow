/**
 * Project: swallow-producerclient
 * 
 * File Created at 2012-6-25
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.swallow.producer.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.dpsf.api.ProxyFactory;
import com.dianping.dpsf.exception.NetException;
import com.dianping.swallow.common.internal.packet.PktProducerGreet;
import com.dianping.swallow.common.internal.producer.ProducerSwallowService;
import com.dianping.swallow.common.internal.util.IPUtil;
import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.producer.exceptions.RemoteServiceInitFailedException;
import com.dianping.swallow.common.producer.exceptions.ServerDaoException;
import com.dianping.swallow.common.producer.exceptions.TopicNameInvalidException;
import com.dianping.swallow.producer.Producer;
import com.dianping.swallow.producer.ProducerConfig;
import com.dianping.swallow.producer.ProducerFactory;
import com.dianping.swallow.producer.ProducerMode;
import com.dianping.swallow.producer.impl.internal.ProducerImpl;
import com.dianping.swallow.producer.impl.internal.SwallowPigeonConfiguration;

/**
 * 实现ProducerFactory接口的工厂类
 * 
 * @author tong.song
 */
public class ProducerFactoryImpl implements ProducerFactory {

   private static ProducerFactoryImpl instance;                                                            //Producer工厂类单例

   private final Logger               logger          = LoggerFactory.getLogger(ProducerFactoryImpl.class);
   private final String               producerIP      = IPUtil.getFirstNoLoopbackIP4Address();             //Producer IP地址
   private final String               producerVersion = "0.6.0";                                           //Producer版本号

   //远程调用相关设置
   private static int                 remoteServiceTimeout;                                                //远程调用超时

   //远程调用相关变量
   @SuppressWarnings("rawtypes")
   private final ProxyFactory         pigeon          = new ProxyFactory();                                //pigeon代理对象
   private ProducerSwallowService     remoteService;                                                       //远程调用对象
   private SwallowPigeonConfiguration pigeonConfigure;

   /**
    * Producer工厂类构造函数
    * 
    * @param timeout 远程调用超时
    * @throws RemoteServiceInitFailedException 远程调用初始化失败
    */
   private ProducerFactoryImpl() throws RemoteServiceInitFailedException {
      //初始化远程调用
      setPigeonConfigure(new SwallowPigeonConfiguration("swallow-producerclient-pigeon.properties"));
      remoteServiceTimeout = getPigeonConfigure().getTimeout();
      remoteService = initPigeon(getPigeonConfigure());
   }

   /**
    * 供Spring使用的初始化函数
    * 
    * @throws RemoteServiceInitFailedException 初始化失败抛出此异常
    */
   public void init() throws RemoteServiceInitFailedException {
      remoteServiceTimeout = getPigeonConfigure().getTimeout();
      remoteService = initPigeon(getPigeonConfigure());
   }

   /**
    * 初始化远程调用服务（pigeon）
    * 
    * @param remoteServiceTimeout 远程调用超时：APP可以忍受的远程调用的最长返回时间
    * @return 实现MQService接口的类，此版本中为pigeon返回的一个远程调用服务代理
    * @throws RemoteServiceInitFailedException 远程调用服务（pigeon）初始化失败
    */
   private ProducerSwallowService initPigeon(SwallowPigeonConfiguration pigeonConfigure)
         throws RemoteServiceInitFailedException {

      pigeon.setIface(ProducerSwallowService.class);
      pigeon.setCallMethod("sync");

      pigeon.setServiceName(pigeonConfigure.getServiceName());
      pigeon.setSerialize(pigeonConfigure.getSerialize());
      pigeon.setTimeout(pigeonConfigure.getTimeout());
      pigeon.setUseLion(pigeonConfigure.isUseLion());
      pigeon.setHosts(pigeonConfigure.getHosts());
      pigeon.setWeight(pigeonConfigure.getWeights());

      ProducerSwallowService remoteService = null;
      try {
         pigeon.init();
         logger.info("[Initialize pigeon successfully.]");

         remoteService = (ProducerSwallowService) pigeon.getProxy();
         logger.info("[Get remoteService successfully.]:[" + "RemoteService's timeout is: " + remoteServiceTimeout
               + ".]");
      } catch (Exception e) {
         logger.error("[Initialize remote service failed.]", e);
         throw new RemoteServiceInitFailedException(e);
      }
      return remoteService;
   }

   /**
    * 获取Producer工厂类单例的函数
    * 
    * @param timeout 远程调用超时，如果小于零，则使用默认超时
    * @return Producer工程类单例
    * @throws RemoteServiceInitFailedException 远程调用初始化失败
    */
   public static synchronized ProducerFactoryImpl getInstance() throws RemoteServiceInitFailedException {
      if (instance == null)
         instance = new ProducerFactoryImpl();
      return instance;
   }

   /**
    * @return 获取远程调用服务接口
    */
   public ProducerSwallowService getRemoteService() {
      return remoteService;
   }

   public void setRemoteService(ProducerSwallowService remoteService) {
      this.remoteService = remoteService;
   }

   /**
    * @return 获取Producer本机IP地址
    */
   public String getProducerIP() {
      return producerIP;
   }

   /**
    * @return 获取Producer的版本号
    */
   public String getProducerVersion() {
      return producerVersion;
   }

   public static int getRemoteServiceTimeout() {
      return remoteServiceTimeout;
   }

   /**
    * 获取默认配置的Producer，默认Producer工作模式为同步，重试次数为5
    * 
    * @throws TopicNameInvalidException topic名称非法//topic名称只能由字母、数字、下划线组成
    * @throws RemoteServiceInitFailedException Producer尝试连接远程服务失败
    */
   @Override
   public Producer createProducer(Destination dest) throws TopicNameInvalidException {
      return createProducer(dest, new ProducerConfig());
   }

   /**
    * 获取Producer实现类对象，通过Map指定Producer的选项，未指定的项使用Producer默认配置， Producer默认配置如下：
    * producerMode:ProducerMode.SYNC_MODE; threadPoolSize:10;
    * continueSend:false; retryTimes:5
    * 
    * @throws TopicNameInvalidException topic名称非法//topic名称只能由字母、数字、下划线组成
    * @throws RemoteServiceInitFailedException Producer尝试连接远程服务失败
    */
   @Override
   public Producer createProducer(Destination dest, ProducerConfig config) throws TopicNameInvalidException {
      ProducerImpl producerImpl = null;
      try {
         producerImpl = new ProducerImpl(this, dest, config);
         logger.info("[New producer instance was created.]:[topicName="
               + dest.getName()
               + "][ProducerMode="
               + producerImpl.getProducerConfig().getMode()
               + "][RetryTimes="
               + producerImpl.getProducerConfig().getRetryTimes()
               + "][IfZipMessage="
               + producerImpl.getProducerConfig().isZipped()
               + (producerImpl.getProducerConfig().getMode().equals(ProducerMode.ASYNC_MODE) ? "][ThreadPoolSize="
                     + producerImpl.getProducerConfig().getThreadPoolSize() + "][IfContinueSend="
                     + producerImpl.getProducerConfig().isSendMsgLeftLastSession() + "]" : "]"));
      } catch (TopicNameInvalidException e) {
         logger.error(
               "[Can not get producer instance.]:[topicName="
                     + dest.getName()
                     + "][ProducerMode="
                     + config.getMode()
                     + "][RetryTimes="
                     + config.getRetryTimes()
                     + "][IfZipMessage="
                     + config.isZipped()
                     + ((config.getMode() == ProducerMode.ASYNC_MODE) ? "][ThreadPoolSize="
                           + config.getThreadPoolSize() + "][IfContinueSend=" + config.isSendMsgLeftLastSession() + "]"
                           : "]"), e);
         throw e;
      }

      //向swallow发送greet信息
      PktProducerGreet pktProducerGreet = new PktProducerGreet(producerVersion, producerIP);

      try {
         remoteService.sendMessage(pktProducerGreet);
      } catch (ServerDaoException e) {
         //一定不会捕获到该异常
      } catch (NetException e) {
         //网络异常，只记录，不抛出，以保证用户可以拿到Producer
         logger.warn("[Network error, couldn't send greet now.]");
      }
      return producerImpl;
   }

   public SwallowPigeonConfiguration getPigeonConfigure() {
      return pigeonConfigure;
   }

   public void setPigeonConfigure(SwallowPigeonConfiguration pigeonConfigure) {
      this.pigeonConfigure = pigeonConfigure;
   }
}
