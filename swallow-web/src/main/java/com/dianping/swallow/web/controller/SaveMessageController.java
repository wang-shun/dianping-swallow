package com.dianping.swallow.web.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.dao.impl.mongodb.MessageDAOImpl;
import com.dianping.swallow.common.internal.message.SwallowMessage;

@SuppressWarnings("unused")
@Controller
@Component
public class SaveMessageController {
	
	private static final String                			V                  			="0.7.1";
	private static final String                			RETRANSMIT                  ="retransmit";
	private static 		 MessageDAO 					mdi;
	
	static{
		@SuppressWarnings("resource")
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		mdi = (MessageDAOImpl) ctx.getBean("messageDAO");
	}
	
	private static final Logger logger = LoggerFactory.getLogger(TopicController.class);
	
	@RequestMapping(value = "/console/message/sendmessage", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public void retransmit(@RequestParam(value = "param[]") String[] param,
							@RequestParam("topic") String topic) {
		String topicName = topic.trim();
		int length = param.length;
		
		if(length <= 0)
			return;
		if(length == 1){
			String tmpString = param[0].trim();
			String subString = tmpString.substring(1,tmpString.length()-1);
			long tmplong = Long.parseLong(subString.replace("\"", ""));
			doGetAndSaveMessage(topicName, tmplong);
		}
		else{
			for(int i = 0; i < length; ++i){
				if(i != 0 && i != length - 1){
					String tmp = param[i].trim().replace("\"", "");
					long tmplong = Long.parseLong(tmp);
					doGetAndSaveMessage(topicName, tmplong);
				}
				else if(i == 0){
					String tmp = param[i].trim().replace("\"", "");
					long tmplong = Long.parseLong(tmp.substring(1));
					doGetAndSaveMessage(topicName, tmplong);
				} 
				else{
					String tmp = param[i].trim().replace("\"", "");
					long tmplong = Long.parseLong(tmp.substring(0, tmp.length()-1));
					doGetAndSaveMessage(topicName, tmplong);
				}
			}
		}
		
		return;
	}
	
	private boolean doGetAndSaveMessage(String topic, long mid){
		SwallowMessage sm = new SwallowMessage();
		sm = mdi.getMessage(topic, mid);
		if(sm != null){  //already exists
			mdi.retransmitMessage(topic, sm);
			return true;
		}
		else{
			if(logger.isInfoEnabled())
				logger.info(topic + " is not in database!");
			return false;
		}
	}
	
	
	@RequestMapping(value = "/console/message/sendgroupmessage", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	@ResponseBody
	public void sendGroupMessages(@RequestParam(value = "name") String topic,
							@RequestParam("textarea") String text,HttpServletRequest request,
							HttpServletResponse response) {
		String topicName = topic.trim();
		String textarea  = text.trim();
		if(textarea.isEmpty()){
			if(logger.isInfoEnabled())
				logger.info(topicName + " is not in whitelist!");
			return;
		}
		String[] contents = textarea.split("\\n");
		int pieces = contents.length;
		if(pieces == 0){
			if(logger.isInfoEnabled())
				logger.info(text + " parses failed because of wrong delimitor, please use '\n' to enter line!");
			return;
		}
		for(int i = 0; i < pieces; ++i){
			saveNewMessage(topicName, contents[i]);
		}
		return;
	}
	
	private void saveNewMessage(String topicName, String content){
		SwallowMessage sm = new SwallowMessage();
		setMessageProperty(sm, content);
		mdi.saveMessage(topicName, sm);
	}
	
	private void setMessageProperty(SwallowMessage sm, String c){
		sm.setContent(c);
		sm.setVersion(V);
		sm.setGeneratedTime(new Date());
		InetAddress addr = null;
		String ip = null;
		try {
			addr = InetAddress.getLocalHost();
			ip = addr.getHostAddress().toString();//获得本机IP
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			ip = "127.0.0.1";
			e.printStackTrace();
		}
		
		sm.setSourceIp(ip);
		if(sm.getInternalProperties() != null)
			sm.getInternalProperties().put(RETRANSMIT, "true");
		else{
			Map<String,String> map = new HashMap<String, String>();
			map.put(RETRANSMIT, "true");
			sm.setInternalProperties(map);
		}
	}
}
