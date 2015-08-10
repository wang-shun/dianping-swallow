package com.dianping.swallow.web.model.alarm;

import java.util.Date;

import org.springframework.data.annotation.Transient;

public class AlarmMeta {
	
	@Transient
	public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

	@Transient
	public static final String IP_TEMPLATE = "{ip}";

	@Transient
	public static final String DATE_TEMPLATE = "{date}";

	@Transient
	public static final String CURRENTVALUE_TEMPLATE = "{currentValue}";

	@Transient
	public static final String EXPECTEDVALUE_TEMPLATE = "{expectedValue}";

	@Transient
	public static final String MASTERIP_TEMPLATE = "{masterIp}";

	@Transient
	public static final String SLAVEIP_TEMPLATE = "{slaveIp}";

	@Transient
	public static final String TOPIC_TEMPLATE = "{topic}";

	@Transient
	public static final String CONSUMERID_TEMPLATE = "{consumerId}";
	
	private String id;

	private int metaId;

	private AlarmType type;

	private AlarmLevelType levelType;

	private boolean isSmsMode;

	private boolean isWeiXinMode;

	private boolean isMailMode;

	private boolean isSendSwallow;

	private boolean isSendBusiness;

	private String alarmTitle;

	private String alarmTemplate;
	
	private String alarmDetail;
	
	private int timeSpanBase;
	
	private int maxTimeSpan;
	
	private int daySpanRatio;
	
	private int nightSpanRatio;

	private int sendTimeSpan;

	private Date createTime;

	private Date updateTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getMetaId() {
		return metaId;
	}

	public void setMetaId(int metaId) {
		this.metaId = metaId;
	}

	public AlarmType getType() {
		return type;
	}

	public void setType(AlarmType type) {
		this.type = type;
	}

	public AlarmLevelType getLevelType() {
		return levelType;
	}

	public void setLevelType(AlarmLevelType levelType) {
		this.levelType = levelType;
	}

	public boolean getIsSmsMode() {
		return isSmsMode;
	}

	public void setIsSmsMode(boolean isSmsMode) {
		this.isSmsMode = isSmsMode;
	}

	public boolean getIsWeiXinMode() {
		return isWeiXinMode;
	}

	public void setIsWeiXinMode(boolean isWeiXinMode) {
		this.isWeiXinMode = isWeiXinMode;
	}

	public boolean getIsMailMode() {
		return isMailMode;
	}

	public void setIsMailMode(boolean isMailMode) {
		this.isMailMode = isMailMode;
	}

	public boolean getIsSendSwallow() {
		return isSendSwallow;
	}

	public void setIsSendSwallow(boolean isSendSwallow) {
		this.isSendSwallow = isSendSwallow;
	}

	public boolean getIsSendBusiness() {
		return isSendBusiness;
	}

	public void setIsSendBusiness(boolean isSendBusiness) {
		this.isSendBusiness = isSendBusiness;
	}
	
	public String getAlarmTitle() {
		return alarmTitle;
	}

	public void setAlarmTitle(String alarmTitle) {
		this.alarmTitle = alarmTitle;
	}

	public String getAlarmTemplate() {
		return alarmTemplate;
	}

	public void setAlarmTemplate(String alarmTemplate) {
		this.alarmTemplate = alarmTemplate;
	}
	
	public String getAlarmDetail() {
		return alarmDetail;
	}

	public void setAlarmDetail(String alarmDetail) {
		this.alarmDetail = alarmDetail;
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

	public int getSendTimeSpan() {
		return sendTimeSpan;
	}

	public void setSendTimeSpan(int sendTimeSpan) {
		this.sendTimeSpan = sendTimeSpan;
	}

	@Override
	public String toString() {
		return "AlarmMeta [id=" + id + ", metaId=" + metaId + ", type=" + type + ", levelType=" + levelType
				+ ", isSmsMode=" + isSmsMode + ", isWeiXinMode=" + isWeiXinMode + ", isMailMode=" + isMailMode
				+ ", isSendSwallow=" + isSendSwallow + ", isSendBusiness=" + isSendBusiness + ", alarmTemplate="
				+ alarmTemplate + ", sendTimeSpan=" + sendTimeSpan + ", createTime=" + createTime + ", updateTime="
				+ updateTime + "]";
	}

	public int getTimeSpanBase() {
		return timeSpanBase;
	}

	public void setTimeSpanBase(int timeSpanBase) {
		this.timeSpanBase = timeSpanBase;
	}

	public int getMaxTimeSpan() {
		return maxTimeSpan;
	}

	public void setMaxTimeSpan(int maxTimeSpan) {
		this.maxTimeSpan = maxTimeSpan;
	}

	public int getDaySpanRatio() {
		return daySpanRatio;
	}

	public void setDaySpanRatio(int daySpanRatio) {
		this.daySpanRatio = daySpanRatio;
	}

	public int getNightSpanRatio() {
		return nightSpanRatio;
	}

	public void setNightSpanRatio(int nightSpanRatio) {
		this.nightSpanRatio = nightSpanRatio;
	}

}
