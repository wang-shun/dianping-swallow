package com.dianping.swallow.web.controller.dto;


/**
 * @author mingdongli
 *
 *         2015年8月27日下午4:11:02
 */
public class IpResourceDto {

	private String id;

	private String ip;

	private boolean alarm;

	private String application;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public boolean isAlarm() {
		return alarm;
	}

	public void setAlarm(boolean alarm) {
		this.alarm = alarm;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

}
