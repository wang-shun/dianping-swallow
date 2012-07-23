package com.dianping.swallow.producer.cases;

import java.util.HashMap;
import java.util.Map;

import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.producer.exceptions.SendFailedException;
import com.dianping.swallow.producer.ProducerConfig;
import com.dianping.swallow.producer.ProducerMode;
import com.dianping.swallow.producer.impl.ProducerFactoryImpl;
import com.dianping.swallow.producer.impl.internal.ProducerImpl;

public class SyncSendMessageWithMulitipleThread {
   private ProducerFactoryImpl producerFactory = null;
   private String              message         = "";

   //初始化
   public SyncSendMessageWithMulitipleThread() {
      //初始化ProducerFactory
      try {
         producerFactory = ProducerFactoryImpl.getInstance();
      } catch (Exception e) {
      }
      //设置待发送的消息内容
      for (int i = 0; i < 10; i++) {
         message += "AAbbCCddEEffGGhhII jKKllMMnnOOppQQrr SttUUvvWWxxYYzz11@@33$$55^^77**99))aaeeeffggesswweedd!@#$%^&*()";
      }

   }

   private class TestTask implements Runnable {
      private String pre;
      private int freq;
      public TestTask(String pre, int freq){
         this.pre = pre;
         this.freq = freq;
      }
      @Override
      public void run() {
         //设置Producer选项
         ProducerConfig config = new ProducerConfig();
         config.setMode(ProducerMode.SYNC_MODE);
         config.setRetryTimes(3);
         config.setThreadPoolSize(3);
         config.setSendMsgLeftLastSession(false);

         //设置发送消息时的选项
         Map<String, String> properties = new HashMap<String, String>();
         properties.put("test", "true");

         //构造Producer
         ProducerImpl producer = null;
         try {
            producer = (ProducerImpl) producerFactory.createProducer(Destination.topic("songtong"), config);
         } catch (Exception e) {
            e.printStackTrace();
         }

         //发送消息设置
         final int MAX_NUM = 100; //可发送消息的最大数量
         int sentNum = 0; //已发送消息数量
         String strRet = ""; //发送消息的返回值
         int i = 0;

         //发送消息
//         while (true) {
         for (i = 0, strRet = ""; i < MAX_NUM; i++) {
            try {
               //发送消息
               strRet = producer.sendMessage( pre + (++sentNum));
            } catch (SendFailedException e) {
               e.printStackTrace();
            }
            //发送频率
            try {
               Thread.sleep(freq);
            } catch (Exception e) {
            }

            //打印内容
            System.out.println("\t" + sentNum + ": " + strRet);
         }
      }
   }

   public void doTest() {
      final int THREAD_NUM = 1;//线程数量

      for (int i = 0; i < THREAD_NUM; i++) {
         Thread task = new Thread(new TestTask("thread " + i + ": ", (i+1)*1000));
         task.start();
         try{
            Thread.sleep(5000);
         }catch(Exception e){
         }
      }
   }

   public static void main(String[] args) {
      new SyncSendMessageWithMulitipleThread().doTest();
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}