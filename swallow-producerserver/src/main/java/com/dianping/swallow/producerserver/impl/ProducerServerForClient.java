package com.dianping.swallow.producerserver.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dianping.dpsf.api.ServiceRegistry;
import com.dianping.hawk.jmx.HawkJMXUtil;
import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.internal.packet.Packet;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.packet.PktProducerGreet;
import com.dianping.swallow.common.internal.packet.PktSwallowPACK;
import com.dianping.swallow.common.internal.producer.SwallowService;
import com.dianping.swallow.common.internal.util.IPUtil;
import com.dianping.swallow.common.internal.util.SHAUtil;
import com.dianping.swallow.common.producer.exceptions.RemoteServiceInitFailedException;
import com.dianping.swallow.common.producer.exceptions.ServerDaoException;

public class ProducerServerForClient implements SwallowService {

   private static final Logger logger             = Logger.getLogger(ProducerServerForClient.class);
   private static final int    DEFAULT_PORT       = 4000;
   public static final String  producerServerIP   = IPUtil.getFirstNoLoopbackIP4Address();

   private int                 port               = DEFAULT_PORT;
   private long                receivedMessageNum = 0;
   private MessageDAO          messageDAO;

   public ProducerServerForClient() {
      //Hawk监控
      HawkJMXUtil.registerMBean("ProducerServerForClient", new HawkMBean());
   }

   /**
    * 启动producerServerClient
    * 
    * @param port 供producer连接的端口
    * @throws RemoteServiceInitFailedException 远程调用初始化失败
    * @throws Exception 连续绑定同一个端口抛出异常，pigeon初始化失败抛出异常
    */
   public void start() throws RemoteServiceInitFailedException {
      try {
         ServiceRegistry remoteService = null;
         remoteService = new ServiceRegistry(getPort());
         Map<String, Object> services = new HashMap<String, Object>();
         services.put("remoteService", this);
         remoteService.setServices(services);
         remoteService.init();
         logger.info("[Initialize pigeon sucessfully, Producer service for client is ready.]");
      } catch (Exception e) {
         logger.error("[Initialize pigeon failed.]", e);
         throw new RemoteServiceInitFailedException();
      }
   }

   /**
    * 保存swallowMessage到数据库
    * 
    * @throws ServerDaoException
    */
   @Override
   public Packet sendMessage(Packet pkt) throws ServerDaoException {
      Packet pktRet = null;
      SwallowMessage swallowMessage;
      String topicName;
      String sha1;
      switch (pkt.getPacketType()) {
         case PRODUCER_GREET:
            logger.info("[Got Greet][From=" + ((PktProducerGreet) pkt).getProducerIP() + "][Version="
                  + ((PktProducerGreet) pkt).getProducerVersion() + "]");
            //返回ProducerServer地址
            pktRet = new PktSwallowPACK(producerServerIP);
            break;
         case OBJECT_MSG:
            swallowMessage = ((PktMessage) pkt).getContent();
            topicName = ((PktMessage) pkt).getDestination().getName();
            sha1 = SHAUtil.generateSHA(swallowMessage.getContent());
            pktRet = new PktSwallowPACK(sha1);
            //设置swallowMessage的sha-1
            swallowMessage.setSha1(sha1);

            //将swallowMessage保存到mongodb
            try {
               messageDAO.saveMessage(topicName, swallowMessage);
            } catch (Exception e) {
               logger.error("[Save message to DB failed.]", e);
               throw new ServerDaoException();
            }
            break;
         default:
            logger.warn("[Received unrecognized packet.]");
            break;
      }
      return pktRet;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public void setMessageDAO(MessageDAO messageDAO) {
      this.messageDAO = messageDAO;
   }

   /**
    * 用于Hawk监控
    */
   public class HawkMBean {
      public String getProducerserverip() {
         return producerServerIP;
      }

      public long getReceivedMessageNum() {
         return receivedMessageNum;
      }

      public int getPort() {
         return port;
      }
   }

}
