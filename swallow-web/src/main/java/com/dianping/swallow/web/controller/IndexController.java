package com.dianping.swallow.web.controller;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.dianping.swallow.web.controller.utils.ExtractUsernameUtils;
import com.dianping.swallow.web.service.UserService;


@Controller
public class IndexController extends AbstractMenuController {
	
	private AtomicBoolean isAdmin = new AtomicBoolean();
	@Resource(name = "userService")
	private UserService userService;
	
	@Autowired
	private ExtractUsernameUtils extractUsernameUtils;

	@RequestMapping(value = "/")
	public ModelAndView allApps(HttpServletRequest request,
			HttpServletResponse response) {
		
		String username = extractUsernameUtils.getUsername(request);
		Set<String> adminSet = userService.loadCachedAdministratorSet();
		boolean admin = adminSet.contains(username);
		isAdmin.set(admin);
		
		if(admin){
			return new ModelAndView("server/producer", createViewMap());
		}else{
			return new ModelAndView("topic/index", createViewMap());
		}
	    
	}

	@Override
	protected String getMenu() {
		if(isAdmin.get()){
			return "server";
		}else{
			return "topic";
		}
	}
	
}
