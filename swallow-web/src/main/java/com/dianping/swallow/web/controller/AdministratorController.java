package com.dianping.swallow.web.controller;

import com.dianping.swallow.web.controller.dto.UserQueryDto;
import com.dianping.swallow.web.controller.utils.UserUtils;
import com.dianping.swallow.web.model.Administrator;
import com.dianping.swallow.web.model.UserType;
import com.dianping.swallow.web.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mingdongli 2015年5月5日 下午2:42:57
 */
@Controller
public class AdministratorController extends AbstractMenuController {

	@Resource(name = "userService")
	private UserService userService;

	@Autowired
	UserUtils extractUsernameUtils;

	@RequestMapping(value = "/console/administrator")
	public ModelAndView administrator() {

		return new ModelAndView("admin/index", createViewMap());
	}

	@RequestMapping(value = "/console/admin/auth/userlist", method = RequestMethod.POST)
	@ResponseBody
	public Object userList(@RequestBody UserQueryDto userQueryDto) {

		return userService.loadUserPage(userQueryDto.getOffset(), userQueryDto.getLimit());
	}

	@RequestMapping(value = "/console/admin/auth/createadmin", method = RequestMethod.POST)
	@ResponseBody
	public void createAdmin(@RequestBody UserQueryDto userQueryDto) {

		String name = userQueryDto.getName();
		UserType tpye = UserType.findByType(userQueryDto.getRole().trim());
		userService.createUser(name, tpye);
	}

	@RequestMapping(value = "/console/admin/auth/removeadmin", method = RequestMethod.POST)
	@ResponseBody
	public void removeAdmin(@RequestBody UserQueryDto userQueryDto) {

		String name = userQueryDto.getName();
		userService.removeUser(name);
	}

	@RequestMapping(value = "/console/admin/queryvisits", method = RequestMethod.GET)
	@ResponseBody
	public Object queryAllVisits() {

		List<Administrator> adminList = userService.findAll();
		List<String> users = new ArrayList<String>();
		for (Administrator admin : adminList) {
			String name = admin.getName();
			if (StringUtils.isNotBlank(name) && !users.contains(name)) {
				users.add(name);
			}
		}
		return users;
	}

	@Override
	protected String getMenu() {

		return "admin";
	}

}
