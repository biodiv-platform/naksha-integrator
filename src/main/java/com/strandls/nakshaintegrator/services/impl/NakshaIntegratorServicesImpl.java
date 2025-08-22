/**
 * 
 */
package com.strandls.nakshaintegrator.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.authentication_utility.util.PropertyFileUtil;
import com.strandls.nakshaintegrator.Headers;
import com.strandls.nakshaintegrator.services.MailService;
import com.strandls.nakshaintegrator.services.NakshaIntegratorServices;
import com.strandls.nakshaintegrator.util.Utils;
import com.strandls.user.ApiException;
import com.strandls.user.controller.UserServiceApi;
import com.strandls.user.pojo.DownloadLogData;
import com.strandls.user.pojo.User;
import com.strandls.user.pojo.UserIbp;

import net.minidev.json.JSONArray;

public class NakshaIntegratorServicesImpl implements NakshaIntegratorServices {

	@Inject
	private UserServiceApi userServiceApi;

	@Inject
	private MailService mailService;

	@Inject
	private Headers headers;

	private final Logger logger = LoggerFactory.getLogger(NakshaIntegratorServicesImpl.class);

	private byte[] getRequest(String uri, List<NameValuePair> params) {

		CloseableHttpResponse response = null;
		CloseableHttpClient httpclient = null;
		byte[] byteArrayResponse = null;

		String host = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiHost");
		String portalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");
		String apikey = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiKey");
		String scheme = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiScheme");

		try {

			// String url =
			// "https://staging.communityconservedareas.org/naksha-api/api/layer/all?limit=10&offset=0";

			URIBuilder builder = new URIBuilder();
			builder.setScheme(scheme).setHost(host).setPath(uri);

			if (params != null)
				builder.setParameters(params);
			URI Uri = null;
			Uri = builder.build();
			HttpGet request = new HttpGet(Uri);

			request.setHeader("Portal-Id", portalId);
			request.setHeader("api-key", apikey);

			httpclient = HttpClients.createDefault();

			response = httpclient.execute(request);

			HttpEntity entity = response.getEntity();

			byteArrayResponse = EntityUtils.toByteArray(entity);
			EntityUtils.consume(entity);

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Error while trying to send request at URL {}");
		} finally {
			if (byteArrayResponse != null)
				HttpClientUtils.closeQuietly(response);
			try {
				if (httpclient != null)
					httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

		return byteArrayResponse != null ? byteArrayResponse : new byte[0];
	}

	public byte[] postRequestWithMultipartFormData(String uri, List<NameValuePair> params, FormDataMultiPart multiPart,
			Map<String, Object> payload) {
		CloseableHttpResponse response = null;
		CloseableHttpClient httpclient = null;
		byte[] byteArrayResponse = null;
		String host = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiHost");
		String portalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");
		String apikey = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiKey");
		String scheme = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiScheme");

		try {

			URIBuilder builder = new URIBuilder();
			builder.setScheme(scheme).setHost(host).setPath(uri);
			if (params != null)
				builder.setParameters(params);

			HttpPost request = new HttpPost(builder.build());

			request.setHeader("Portal-Id", portalId);
			request.setHeader("api-key", apikey);

			if (multiPart != null) {
				MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
				entityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);

				for (Map.Entry<String, List<FormDataBodyPart>> entry : multiPart.getFields().entrySet()) {
					String fieldName = entry.getKey();
					for (FormDataBodyPart field : entry.getValue()) {
						if (field.getEntity() instanceof BodyPartEntity) {
							BodyPartEntity bodyPartEntity = (BodyPartEntity) field.getEntity();
							InputStream inputStream = bodyPartEntity.getInputStream();
							String fileName = field.getContentDisposition().getFileName();
							entityBuilder.addPart(fieldName,
									new InputStreamBody(inputStream, ContentType.APPLICATION_OCTET_STREAM, fileName));
						} else {
							entityBuilder.addTextBody(fieldName, field.getValue(), ContentType.TEXT_PLAIN);
						}
					}
				}

				HttpEntity multipartEntity = entityBuilder.build();
				request.setEntity(multipartEntity);
			}

			if (payload != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				String jsonBody = objectMapper.writeValueAsString(payload);
				StringEntity entity = new StringEntity(jsonBody);
				request.setHeader("Content-Type", "application/json");
				request.setEntity(entity);

			}

			httpclient = HttpClients.createDefault();
			response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();

			byteArrayResponse = EntityUtils.toByteArray(entity);
			EntityUtils.consume(entity);

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Error while trying to send request at URL {}", uri);
		} finally {
			if (byteArrayResponse != null)
				HttpClientUtils.closeQuietly(response);
			try {
				if (httpclient != null)
					httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

		return byteArrayResponse != null ? byteArrayResponse : new byte[0];
	}

	private byte[] putRequest(String uri, List<NameValuePair> params) {

		CloseableHttpResponse response = null;
		CloseableHttpClient httpclient = null;
		byte[] byteArrayResponse = null;

		String host = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiHost");
		String portalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");
		String apikey = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiKey");
		String scheme = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiScheme");
		try {

			// String url =
			// "https://staging.communityconservedareas.org/naksha-api/api/layer/all?limit=10&offset=0";

			URIBuilder builder = new URIBuilder();
			builder.setScheme(scheme).setHost(host).setPath(uri);

			if (params != null)
				builder.setParameters(params);
			URI Uri = null;
			Uri = builder.build();
			HttpPut request = new HttpPut(Uri);

			request.setHeader("Portal-Id", portalId);
			request.setHeader("api-key", apikey);

			httpclient = HttpClients.createDefault();

			response = httpclient.execute(request);

			HttpEntity entity = response.getEntity();

			byteArrayResponse = EntityUtils.toByteArray(entity);
			EntityUtils.consume(entity);

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Error while trying to send request at URL {}");
		} finally {
			if (byteArrayResponse != null)
				HttpClientUtils.closeQuietly(response);
			try {
				if (httpclient != null)
					httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

		return byteArrayResponse != null ? byteArrayResponse : new byte[0];
	}

	@Override
	public List<HashMap<String, Object>> getTOCList(HttpServletRequest request, Integer limit, Integer offset,
			boolean showOnlyPending) {

		CommonProfile userProfile = AuthUtil.getProfileFromRequest(request);
		List<HashMap<String, Object>> metaLayers = null;

		List<HashMap<String, Object>> layerLists = new ArrayList<>();
		boolean isAdmin = Utils.isAdmin(request);

		ArrayList<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("offset", offset.toString()));
		params.add(new BasicNameValuePair("limit", limit.toString()));

		byte[] ans = getRequest("/naksha-api/api/layer/all", params);

		ObjectMapper mapper = new ObjectMapper();
		try {
			metaLayers = mapper.readValue(ans, new TypeReference<List<HashMap<String, Object>>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			for (HashMap<String, Object> metaLayer : metaLayers) {

				if ((!isAdmin && "Pending".equals(metaLayer.get("layerStatus")))
						|| (showOnlyPending && !"Pending".equals(metaLayer.get("layerStatus"))))
					continue;

//				String authorId = metaLayer.get("uploaderUserId").toString();
//
//				UserIbp userIbp = userServiceApi.getUserIbp(authorId + "");
//				metaLayer.put("author", userIbp);

				Boolean isDownloadable = checkDownLoadAccess(userProfile, metaLayer);
				metaLayer.put("isDownloadable", isDownloadable);

				layerLists.add(metaLayer);
			}
			return layerLists;

		} catch (Exception e) {
			logger.error(e.getMessage());
			return layerLists;
		}

	}

	@Override
	public Map<String, Object> uploadLayer(HttpServletRequest request, FormDataMultiPart multiPart) {

		Map<String, Object> result = null;
		CommonProfile userProfile = AuthUtil.getProfileFromRequest(request);
		String uploaderUserId = userProfile.getId();

		multiPart.field("uploaderUserId", uploaderUserId, MediaType.TEXT_PLAIN_TYPE);
		byte[] ans = postRequestWithMultipartFormData("/naksha-api/api/layer/upload", null, multiPart, null);
		ObjectMapper mapper = new ObjectMapper();

		try {
			result = mapper.readValue(ans, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public Map<String, Object> getLayerInfo(String layer) {

		String uri = "/naksha-api/api/layer/onClick/" + layer;
		Map<String, Object> result = null;
		byte[] ans = getRequest(uri, null);
		ObjectMapper mapper = new ObjectMapper();
		try {
			result = mapper.readValue(ans, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public Map<String, Object> makeLayerPending(String layerName) {
		String uri = "/naksha-api/api/layer/pending/" + layerName;
		Map<String, Object> result = null;
		byte[] ans = putRequest(uri, null);
		ObjectMapper mapper = new ObjectMapper();
		try {
			result = mapper.readValue(ans, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public Map<String, Object> makeLayerActive(String layerName) {
		String uri = "/naksha-api/api/layer/active/" + layerName;
		Map<String, Object> result = null;
		byte[] ans = putRequest(uri, null);
		ObjectMapper mapper = new ObjectMapper();
		try {
			result = mapper.readValue(ans, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public byte[] downloadShpFile(String hashKey, String layerName) {
		String uri = String.format("/naksha-api/api/layer/download/%s/%s", hashKey, layerName);
		return getRequest(uri, null);
	}

	@Override
	public Map<String, Object> prepareDownloadLayer(HttpServletRequest request, Map<String, Object> layerDownload) {

		Map<String, Object> result = new HashMap<String, Object>();
		String layerName = (String) layerDownload.get("layerName");
		String getMetaDataUri = "/naksha-api/api/layer/" + layerName;

		byte[] metaData = getRequest(getMetaDataUri, null);
		HashMap<String, Object> layerMetaData = null;

		ObjectMapper mapper = new ObjectMapper();

		try {
			layerMetaData = mapper.readValue(metaData, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		CommonProfile profile = AuthUtil.getProfileFromRequest(request);

		if (!checkDownLoadAccess(profile, layerMetaData)) {
			result.put("failed", "User is not authorized to download the layer");
			return result;
		}

		String uri = "/naksha-api/api/layer/download";

		byte[] ans = postRequestWithMultipartFormData(uri, null, null, layerDownload);
		// ObjectMapper mapper = new ObjectMapper();
		try {
			result = mapper.readValue(ans, new TypeReference<HashMap<String, Object>>() {
			});

			String filePath = (String) result.get("filePath");
			String url = "/nakshaIntegrator-api/api/layer/download" + filePath;

			mailService.sendMail(profile.getId(), url, "naksha");
			userServiceApi = headers.addUserHeaders(userServiceApi, request.getHeader(HttpHeaders.AUTHORIZATION));
			DownloadLogData data = new DownloadLogData();
			data.setFilePath(url);
			data.setFileType(layerMetaData.get("layerType").toString() == "RASTER" ? "RASTER" : "SHP");
			data.setFilterUrl(url);
			data.setStatus("success");
			data.setSourcetype("Map");
			data.setNotes(layerDownload.get("layerTitle").toString());
			try {
				userServiceApi.logDocumentDownload(data);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}

			result.put("url", url);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return result;

	}

	private boolean checkDownLoadAccess(CommonProfile profile, Map<String, Object> metaLayer) {
		if (profile == null)
			return false;

		// return true if user is admin, irrespective of the portal
		JSONArray roles = (JSONArray) profile.getAttribute("roles");
		if (roles.contains("ROLE_ADMIN"))
			return true;

		if (metaLayer == null)
			return false;

//		 download access is the property of the portal, hence return true irrespective
//		 of portal
//		 if it is All
		if (metaLayer.get("downloadAccess").toString().equalsIgnoreCase("ALL")) {
			return true;
		} else {
			String uploaderPortalId = metaLayer.get("portalId").toString();
			String loggedinUserPortalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");

			if (!uploaderPortalId.equals(loggedinUserPortalId)) {
				return false;
			} else {
				String uploaderId = metaLayer.get("uploaderUserId").toString();
				String loggedInUserId = profile.getId();
				if (uploaderId.equals(loggedInUserId)) {
					return true;
				} else {
					return false;
				}
			}
		}

	}

	@Override
	public Map<String, Object> getLocationInfo(String lat, String lon) {
		String uri = "/naksha-api/api/layer/locationInfo";

		ArrayList<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("lat", lat));
		params.add(new BasicNameValuePair("lon", lon));

		Map<String, Object> result = null;
		byte[] ans = getRequest(uri, params);
		ObjectMapper mapper = new ObjectMapper();
		try {
			result = mapper.readValue(ans, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return result;

	}

}
