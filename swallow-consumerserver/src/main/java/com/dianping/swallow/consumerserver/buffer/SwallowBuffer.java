package com.dianping.swallow.consumerserver.buffer;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import com.dianping.swallow.common.message.Message;

public class SwallowBuffer {
   /**
    * 锁是为了防止相同topicName的TopicBuffer被并发创建，所以相同的topicName对应相同的锁。
    * 锁的个数也决定了最多能有多少创建TopicBuffer的并发操作
    */
   private final int                                LOCK_NUM_FOR_CREATE_TOPIC_BUFFER = 10;
   private ReentrantLock[]                          locksForCreateTopicBuffer        = new ReentrantLock[LOCK_NUM_FOR_CREATE_TOPIC_BUFFER];
   {
      for (int i = 0; i < locksForCreateTopicBuffer.length; i++) {
         locksForCreateTopicBuffer[i] = new ReentrantLock();
      }
   }
   private final ConcurrentMap<String, TopicBuffer> topicBuffers                     = new ConcurrentHashMap<String, TopicBuffer>();
   private MessageRetriever                         messageRetriever;
   private int                                      capacity                         = Integer.MAX_VALUE;
   private int                                      threshold                        = 100;

   /**
    * 根据topicName，获取topicName对应的TopicBuffer。<br>
    * 如果topicBuffer已经存在，则返回。否则创建一个新的TopicBuffer，该方法保证，
    * 一个topicName只有一个TopicBuffer。<br>
    * 该方法是线程安全的。
    */
   protected TopicBuffer getTopicBuffer(String topicName) {
      TopicBuffer topicBuffer = topicBuffers.get(topicName);
      if (topicBuffer != null) {
         return topicBuffer;
      }
      // topicBuffer不存在，须创建
      ReentrantLock reentrantLock = locksForCreateTopicBuffer[index(topicName)];//对于String对象的加锁，也可以使用synchronized(topicName.inner())
      try {
         reentrantLock.lock();// 加锁，防止同时创建相同topicName的TopicCache
         topicBuffer = topicBuffers.get(topicName);// double check
         if (topicBuffer == null) {
            topicBuffer = new TopicBuffer(topicName);
            topicBuffers.put(topicName, topicBuffer);
         }
      } finally {// 释放锁
         reentrantLock.unlock();
      }
      return topicBuffer;
   }

   /**
    * 根据消费者id，获取消息队列<br>
    * 该方法是线程安全的。
    * 
    * @param cid 消费者id
    * @return 返回消费者id对应的消息队列
    */
   public BlockingQueue<Message> getMessageQueue(String topicName, Long cid) {
      return this.getTopicBuffer(topicName).getMessageQueue(cid);
   }

   /**
    * 创建一个BlockingQueue
    * 
    * @param cid
    * @param tailMessageId 从messageId大于messageIdOfTailMessage的消息开始消费
    * @return
    */
   public BlockingQueue<Message> createMessageQueue(String topicName, String cid, Long tailMessageId,
                                                    Set<String> messageTypeSet) {
      return this.getTopicBuffer(topicName).createMessageQueue(cid, tailMessageId, messageTypeSet);
   }

   /**
    * 创建一个BlockingQueue
    * 
    * @param cid
    * @param tailMessageId 从messageId大于messageIdOfTailMessage的消息开始消费
    * @return
    */
   public BlockingQueue<Message> createMessageQueue(String topicName, String cid, Long tailMessageId) {
      return this.getTopicBuffer(topicName).createMessageQueue(cid, tailMessageId);
   }

   private int index(String topicName) {
      int hashcode = topicName.hashCode();
      hashcode = hashcode == Integer.MIN_VALUE ? 0 : Math.abs(hashcode);// 保证非负
      return hashcode % LOCK_NUM_FOR_CREATE_TOPIC_BUFFER;
   }

   public int getCapacity() {
      return capacity;
   }

   public void setCapacity(int capacity) {
      this.capacity = capacity;
   }

   public int getThreshold() {
      return threshold;
   }

   public void setThreshold(int threshold) {
      this.threshold = threshold;
   }

   public MessageRetriever getMessageRetriever() {
      return messageRetriever;
   }

   public void setMessageRetriever(MessageRetriever messageRetriever) {
      this.messageRetriever = messageRetriever;
   }

   class TopicBuffer {

      private final String                                                     topicName;

      private ConcurrentHashMap<String, SoftReference<BlockingQueue<Message>>> messageQueues = new ConcurrentHashMap<String, SoftReference<BlockingQueue<Message>>>();

      private TopicBuffer(String topicName) {
         this.topicName = topicName;
      }

      /**
       * 根据消费者id，获取消息队列<br>
       * 该方法是线程安全的。
       * 
       * @param cid 消费者id
       * @return 返回消费者id对应的消息队列
       */
      public BlockingQueue<Message> getMessageQueue(Long cid) {
         if (cid == null) {
            throw new IllegalArgumentException("cid is null.");
         }
         Reference<BlockingQueue<Message>> ref = messageQueues.get(cid);
         if (ref == null) {
            return null;
         }
         return ref.get();
      }

      /**
       * 创建一个BlockingQueue
       * 
       * @param cid
       * @param tailMessageId 从messageId大于messageIdOfTailMessage的消息开始消费
       * @return
       */
      public BlockingQueue<Message> createMessageQueue(String cid, Long tailMessageId) {
         return this.createMessageQueue(cid, tailMessageId, null);
      }

      /**
       * 创建一个BlockingQueue
       * 
       * @param cid
       * @param tailMessageId 从messageId大于messageIdOfTailMessage的消息开始消费
       * @return
       */
      public BlockingQueue<Message> createMessageQueue(String cid, Long tailMessageId, Set<String> messageTypeSet) {
         if (cid == null) {
            throw new IllegalArgumentException("cid is null.");
         }
         if (tailMessageId == null) {
            throw new IllegalArgumentException("messageIdOfTailMessage is null.");
         }
         MessageBlockingQueue messageBlockingQueue;
         if (capacity > 0 && messageTypeSet != null) {
            messageBlockingQueue = new MessageBlockingQueue(cid, this.topicName, threshold, capacity, tailMessageId,
                  messageTypeSet);
         } else if (messageTypeSet != null) {
            messageBlockingQueue = new MessageBlockingQueue(cid, this.topicName, threshold, tailMessageId,
                  messageTypeSet);
         } else {
            messageBlockingQueue = new MessageBlockingQueue(cid, this.topicName, threshold, capacity, tailMessageId);
         }
         messageBlockingQueue.setMessageRetriever(messageRetriever);
         messageQueues.put(cid, new SoftReference<BlockingQueue<Message>>(messageBlockingQueue));
         return messageBlockingQueue;
      }

   }

}
