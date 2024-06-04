package com.strandls.nakshaintegrator.controllers;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

	@GET
	@Path("/gwc/service/tms/1.0.0/{layer}/{z}/{x}/{y}")
	@Produces("application/x-protobuf")
	@ApiOperation(value = "Fetch Tiles", notes = "Return Tiles", response = ByteArrayInputStream.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Tiles not found", response = String.class) })
	public Response fetchTiles(@PathParam("layer") String layer, @PathParam("z") String z, @PathParam("x") String x,
			@PathParam("y") String y) {
		byte[] file = geo.getTyles(layer, z, y, x);
		if (file.length > 0) {
			return Response.ok(new ByteArrayInputStream(file)).build();
		} else {
			return Response.status(Response.Status.BAD_REQUEST).entity("Tiles not found").build();
		}
	}

	@GET
	@Path("/thumbnails" + "/{workspace}/{id}")
	@Produces("image/gif")
	@ApiOperation(value = "Fetch Thumbnails", notes = "Return Thumbnails", response = ByteArrayInputStream.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Thumbnail not found", response = String.class) })
	public Response fetchThumbnail(@PathParam("id") String id,
			@DefaultValue("biodiv") @PathParam("workspace") String wspace, @QueryParam("bbox") String para,
			@DefaultValue("200") @QueryParam("width") String width,
			@DefaultValue("200") @QueryParam("height") String height,
			@DefaultValue("EPSG:4326") @QueryParam("srs") String srs) {
		try {
			byte[] file = geo.getThumbnails(id, wspace, para, width, height, srs);
			if (file.length > 0) {
				return Response.ok(new ByteArrayInputStream(file)).build();
			} else {
				return Response.status(Response.Status.BAD_REQUEST).entity("Tiles not found").build();
			}
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}

	}

}
