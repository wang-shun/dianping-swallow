package com.dianping.swallow.web.model.alarm;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;

/**
 * 
 * @author qiyin
 *
 */
public abstract class AbstractServerAlarmSetting {

	@Id
	private String id;

	private String topicWhiteList;

	private List<ServerMachineAlarmSetting> machineAlarmSettings;

	private Date createTime;

	private Date updateTime;

	public List<ServerMachineAlarmSetting> getMachineAlarmSettings() {
		return machineAlarmSettings;
	}

	public void setMachineAlarmSettings(List<ServerMachineAlarmSetting> machineAlarmSettings) {
		this.machineAlarmSettings = machineAlarmSettings;
	}

	@Override
	public String toString() {
		return "AbstractServerAlarmSetting [id = " + id + ", topicWhiteList = " + topicWhiteList
				+ ", machineAlarmSettings = " + machineAlarmSettings + ", createTime = " + createTime
				+ ", updateTime = " + updateTime + "]";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getTopicWhiteList() {
		return topicWhiteList;
	}

	public void setTopicWhiteList(String topicWhiteList) {
		this.topicWhiteList = topicWhiteList;
	}

}
