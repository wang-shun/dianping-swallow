package com.dianping.swallow.web.model.resource;

import java.util.Date;

import org.springframework.data.annotation.Id;


/**
 * @author mingdongli
 *
 * 2015年8月18日下午6:17:12
 */
public class BaseResource {
	
	@Id
	private String id; 
	
	private Date createTime;
	
	private Date updateTime;

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

	@Override
	public String toString() {
		return "BaseResource [id=" + id + ", createTime=" + createTime + ", updateTime=" + updateTime + "]";
	}

}