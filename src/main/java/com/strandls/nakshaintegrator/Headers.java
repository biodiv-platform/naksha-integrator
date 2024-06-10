package com.strandls.nakshaintegrator;

import javax.ws.rs.core.HttpHeaders;

import com.strandls.user.controller.UserServiceApi;

public class Headers {
	public UserServiceApi addUserHeaders(UserServiceApi userService, String authHeader) {
		userService.getApiClient().addDefaultHeader(HttpHeaders.AUTHORIZATION, authHeader);
		return userService;
	}

}
