/**
 * 
 */
package com.strandls.nakshaintegrator.controllers;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import com.strandls.nakshaintegrator.ApiConstants;

import com.strandls.authentication_utility.filter.ValidateUser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Naksha Intergrator Services")
@Path(ApiConstants.V1 + ApiConstants.SERVICES)
public class NakshaIntegratorController {

	@GET
	@ApiOperation(value = "Dummy API Ping", notes = "Checks validity of war file at deployment", response = String.class)
	@Path(ApiConstants.PING)
	@Produces(MediaType.TEXT_PLAIN)
	public String ping() {
		return "pong naksha integrator";
	}

}
