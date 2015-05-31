package com.dianping.swallow.web.service;

import java.util.Map;

import com.dianping.swallow.web.model.Message;


/**
 * @author mingdongli
 *
 * 2015年5月14日下午1:16:39
 */
public interface MessageService extends SwallowService{

	/**
	 * 在限定条件下查询topic指定数量的messages
	 * @param start  	开始位置
	 * @param span	 	偏移量
	 * @param tname     topic名称
	 * @param messageId 消息ID
	 * @param startdt   开始时间
	 * @param stopdt	结束时间	
	 * @param username  用户名
	 */
	Map<String, Object> getMessageFromSpecificTopic(int start, int span, String tname, 
			String messageId, String startdt, String stopdt, String username);
	
	/**
	 * 查询指定消息ID的消息内容
	 * @param topic 消息名称
	 * @param mid	消息ID
	 */
	Message getMessageContent(String topic, String mid);
	
}