package com.strandls.nakshaintegrator.controllers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.strandls.nakshaintegrator.services.GeoserverIntegratorServices;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Geoserver Integrator Services")
@Path("/geoserver")
public class GeoserverIntegratorController {

	@Inject
	private GeoserverIntegratorServices geo;

	@GET
	@Path("/workspaces/{workspaces}" + "/styles" + "/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Fetch Styles", description = "Returns Styles")
	@ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Styles not found") })
	public Response fetchStyle(@PathParam("workspaces") String workspaces, @PathParam("id") String id) {
		try {
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
	@Operation(summary = "Fetch Tiles", description = "Return Tiles")
	@ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Tiles not found") })
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
	@Operation(summary = "Fetch Thumbnails", description = "Return Thumbnails")
	@ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Thumbnail not found") })
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

	@GET
	@Path("/wms")
	@Produces("image/png")
	@Operation(summary = "Fetch Raster", description = "Return Raster")
	@ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Raster not found") })
	public Response fetchRaster(@QueryParam("bbox") String para, @DefaultValue("200") @QueryParam("width") String width,
			@DefaultValue("200") @QueryParam("height") String height,
			@DefaultValue("EPSG:3857") @QueryParam("srs") String srs, @QueryParam("layers") String layers) {
		try {
			byte[] file = geo.getPng(para, width, height, srs, layers);
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
