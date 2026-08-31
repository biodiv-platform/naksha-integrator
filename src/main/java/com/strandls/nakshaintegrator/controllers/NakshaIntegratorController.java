/**
 * 
 */
package com.strandls.nakshaintegrator.controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

//import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.nakshaintegrator.ApiConstants;
import com.strandls.nakshaintegrator.services.NakshaIntegratorServices;
import com.strandls.nakshaintegrator.util.Utils;
import com.strandls.authentication_utility.filter.ValidateUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Naksha Integrator Services")
@Path("/layer")
public class NakshaIntegratorController {

	@Inject
	private NakshaIntegratorServices nakshaIntegratorServices;

	@GET
	@Operation(summary = "Dummy API Ping", description = "Checks validity of war file at deployment")
	@Path(ApiConstants.PING)
	@Produces(MediaType.TEXT_PLAIN)
	public String ping() {
		return "pong naksha integrator";
	}

	@Path("all")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get meta data of all the layers")
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
	@Operation(summary = "Upload Layer", description = "Returns success or failure")
	@ApiResponses(value = { @ApiResponse(responseCode = "400", description = "file not present"),
			@ApiResponse(responseCode = "500", description = "ERROR") })
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
	@Operation(summary = "Get layer information for the layer on click")
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
	@Operation(summary = "Make the layer pending")
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
	@Operation(summary = "Make the layer active")
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
	@Operation(summary = "Download the shp file", description = "Return the shp file")
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
			ContentDisposition contentDisposition = ContentDisposition.type("attachment").fileName(layerName + ".zip")
					.creationDate(new Date()).build();

			return Response.ok(stream).header("Content-Disposition", contentDisposition).build();
		}

	}

	@Path("download")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Prepare shape file", description = "Return the shape file location")
	@ValidateUser
	public Response prepareDownload(@Context HttpServletRequest request,
			@Parameter(description = "layerDownload") Map<String, Object> layerDownload) {
		try {
			Map<String, Object> retValue = nakshaIntegratorServices.prepareDownloadLayer(request, layerDownload);
			return Response.ok().entity(retValue).build();
		} catch (Exception e) {
			Thread.currentThread().interrupt();
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("/locationInfo")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get state, district and tahsil for lat lon")
	public Response fetchLocationInfo(@QueryParam("lat") String lat, @QueryParam("lon") String lon) {
		try {
			Map<String, Object> result = nakshaIntegratorServices.getLocationInfo(lat, lon);
			return Response.ok().entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

}
