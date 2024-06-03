package com.strandls.nakshaintegrator.controllers;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


import com.strandls.nakshaintegrator.services.GeoserverIntegratorServices;
import com.strandls.nakshaintegrator.services.impl.GeoserverIntegratorServicesImpl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Geoserver Intergrator Services")
@Path("/geoserver")
public class GeoserverIntegratorController {

	@Inject
	private GeoserverIntegratorServices geo;

	@GET
	@Path("/workspaces/{workspaces}" + "/styles" + "/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Fetch Styles", notes = "Retruns Styles", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Styles not found", response = String.class) })
	public Response fetchStyle(@PathParam("workspaces") String workspaces, @PathParam("id") String id) {
		try {
			GeoserverIntegratorServicesImpl i = new GeoserverIntegratorServicesImpl();
			String style = geo.getStyles(workspaces, id);
			return Response.status(Status.OK).entity(style).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

}
