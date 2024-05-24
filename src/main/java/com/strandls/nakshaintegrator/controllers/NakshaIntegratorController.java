/**
 * 
 */
package com.strandls.nakshaintegrator.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

//import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.nakshaintegrator.ApiConstants;
import com.strandls.nakshaintegrator.services.NakshaIntegratorServices;
import com.strandls.nakshaintegrator.util.Utils;
import com.strandls.authentication_utility.filter.ValidateUser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Naksha Intergrator Services")
@Path(ApiConstants.V1 + ApiConstants.SERVICES)
public class NakshaIntegratorController {

	@Inject
	private NakshaIntegratorServices nakshaIntegratorServices;

	@GET
	@ApiOperation(value = "Dummy API Ping", notes = "Checks validity of war file at deployment", response = String.class)
	@Path(ApiConstants.PING)
	@Produces(MediaType.TEXT_PLAIN)
	public String ping() {
		return "pong naksha integrator";
	}

	@Path("all")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get meta data of all the layers", response = Object.class, responseContainer = "List")
	public Response findAll(@Context HttpServletRequest request, @DefaultValue("-1") @QueryParam("limit") Integer limit,
			@DefaultValue("-1") @QueryParam("offset") Integer offset,
			@DefaultValue("false") @QueryParam("showOnlyPending") Boolean showOnlyPending) {
		try {
			List<HashMap<String, Object>> layerList = nakshaIntegratorServices.getTOCList(request, limit, offset,
					showOnlyPending);
			return Response.ok().entity(layerList).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("upload")
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Upload Layer", notes = "Returns succuess failure", response = Map.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "file not present", response = String.class),
			@ApiResponse(code = 500, message = "ERROR", response = String.class) })
	// @ValidateUser
	public Response upload(@Context HttpServletRequest request, final FormDataMultiPart multiPart) {
		try {
			Map<String, Object> result = nakshaIntegratorServices.uploadLayer(request, multiPart);
			return Response.ok().entity(result).build();
		} catch (Exception e) {
			Thread.currentThread().interrupt();
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("onClick/{layer}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get layer information for the layer on click", response = Map.class, responseContainer = "List")
	public Response getLayerInfoOnClick(@PathParam("layer") String layer) {
		try {
			Map<String, Object> onClickLayerInfo = nakshaIntegratorServices.getLayerInfo(layer);
			return Response.ok().entity(onClickLayerInfo).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("pending/{layer}")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Make the layer pending", response = Map.class)
	@ValidateUser
	public Response makeLayerPending(@Context HttpServletRequest request, @PathParam("layer") String layer) {
		try {
			if (!Utils.isAdmin(request)) {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can make the layer pending").build());
			}
			Map<String, Object> result = nakshaIntegratorServices.makeLayerPending(layer);
			return Response.ok().entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("active/{layer}")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Make the layer pending", response = Map.class)
	@ValidateUser
	public Response makeLayerActive(@Context HttpServletRequest request, @PathParam("layer") String layer) {
		try {
			if (!Utils.isAdmin(request)) {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can make the layer pending").build());
			}
			Map<String, Object> result = nakshaIntegratorServices.makeLayerActive(layer);
			return Response.ok().entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("download/{hashKey}/{layerName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces("application/zip")
	@ApiOperation(value = "Download the shp file", notes = "Return the shp file", response = StreamingOutput.class)
	public Response download(@PathParam("hashKey") String hashKey, @PathParam("layerName") String layerName) {

		byte[] fileData = nakshaIntegratorServices.downloadShpFile(hashKey, layerName);

		if (fileData.length == 0) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			// Return the file data as a streaming output
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try {
						output.write(fileData);
						output.flush();
					} catch (Exception e) {
						throw new WebApplicationException(
								Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
					}
				}
			};

			// Set the Content-Disposition header to prompt a download dialog in the browser
			ContentDisposition contentDisposition = ContentDisposition.type("attachment")
					.fileName(layerName + ".zip")
					.creationDate(new Date()).build();

			return Response.ok(stream).header("Content-Disposition", contentDisposition).build();
		}

	}

}
