package com.strandls.nakshaintegrator.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.nakshaintegrator.ApiConstants;
import com.strandls.nakshaintegrator.util.MyUpload;
import com.strandls.nakshaintegrator.util.PropertyFileUtil;
import com.strandls.nakshaintegrator.util.TusConfig;
import com.strandls.nakshaintegrator.util.TusResultStore;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;

import org.pac4j.core.profile.CommonProfile;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PathParam;

import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;

@Path("/layer" + ApiConstants.UPLOAD + ApiConstants.TUS)
public class LayerTusUploadController {

	@Inject
	private TusConfig tusConfig;
	@Inject
	private TusResultStore resultStore;

	private final ExecutorService tusFinalizerPool = Executors.newFixedThreadPool(4);
	private volatile boolean tusUploadUriConfigured = false;

	@POST
	@ValidateUser
	public void createTusUpload(@Context HttpServletRequest request, @Context HttpServletResponse response)
			throws IOException {
		processTusRequest(request, response);
	}

	@PATCH
	@Path("/{id}")
	@ValidateUser
	public void patchTusUpload(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@PathParam("id") String id) throws IOException {
		processTusRequest(request, response);
	}

	@POST
	@Path("/{id}")
	@ValidateUser
	public void postTusUploadChunk(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@PathParam("id") String id) throws IOException {
		processTusRequest(request, response);
	}

	@OPTIONS
	@Path("/{id}")
	public Response optionsTusUpload() {
		return Response.ok().build();
	}

	@HEAD
	@Path("/{id}")
	@ValidateUser
	public void headTusUpload(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@PathParam("id") String id) throws IOException {
		processTusRequest(request, response);
	}

	@GET
	@Path("/{id}" + ApiConstants.RESULT)
	@ValidateUser
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTusResult(@Context HttpServletRequest request, @PathParam("id") String id) {
		String requestUri = request.getRequestURI();
		String uploadUri = requestUri.substring(0, requestUri.length() - ApiConstants.RESULT.length());

		TusResultStore.Entry entry = resultStore.get(uploadUri);
		if (entry == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, Object> body = new HashMap<>();
		body.put("complete", entry.complete);
		if (entry.complete) {
			body.put("result", entry.error != null ? Map.of("error", entry.error) : entry.result);
			resultStore.remove(uploadUri);
		}
		return Response.ok(body).build();
	}

	private void processTusRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!tusUploadUriConfigured) {
			synchronized (this) {
				if (!tusUploadUriConfigured) {
					String contextAwareUri = request.getContextPath() + "/api/layer" + ApiConstants.UPLOAD
							+ ApiConstants.TUS;
					tusConfig.getService().withUploadUri(contextAwareUri);
					tusUploadUriConfigured = true;
				}
			}
		}

		CommonProfile profile;
		try {
			profile = AuthUtil.getProfileFromRequest(request);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		String ownerKey = profile.getId();
		String uploadUri = request.getRequestURI();

		try {
			tusConfig.getService().process(request, response, ownerKey);

			UploadInfo info = tusConfig.getService().getUploadInfo(uploadUri, ownerKey);
			if (info != null && info.getOffset().equals(info.getLength())) {
				tusFinalizerPool.submit(() -> finalizeTusUpload(uploadUri, ownerKey, info));
			}
		} catch (TusException | IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void finalizeTusUpload(String uploadUri, String ownerKey, UploadInfo info) {
		TusResultStore.Entry entry = resultStore.getOrCreate(uploadUri);
		try (InputStream uploadedBytes = tusConfig.getService().getUploadedBytes(uploadUri, ownerKey)) {

			String fileName = info.getMetadata().get("filename");
			String hash = info.getMetadata().get("hash");
			String fileRole = info.getMetadata().get("fileRole");

			String basePath = PropertyFileUtil.fetchProperty("config.properties", "tus");
			File dir = new File(basePath + File.separator + hash);
			FileUtils.forceMkdir(dir);
			File dest = new File(dir, fileRole + "_" + fileName);
			FileUtils.copyInputStreamToFile(uploadedBytes, dest);

			// Construct the MyUpload object
			MyUpload upload = new MyUpload();
			upload.setHashKey(hash);
			upload.setFileName(fileName);
			upload.setType(fileRole);
			upload.setPath(dest.getAbsolutePath());
			upload.setDateUploaded(new java.util.Date());
			upload.setFileSize(String.valueOf(dest.length()));

			entry.result = java.util.Collections.singletonList(upload);
			entry.complete = true;

		} catch (Exception e) {
			entry.error = e.getMessage();
			entry.complete = true;
		} finally {
			try {
				tusConfig.getService().deleteUpload(uploadUri, ownerKey);
			} catch (TusException | IOException e) {
			}
		}
	}
}