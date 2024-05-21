/**
 * 
 */
package com.strandls.nakshaintegrator.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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
import com.strandls.nakshaintegrator.services.NakshaIntegratorServices;
import com.strandls.nakshaintegrator.util.Utils;
import com.strandls.user.controller.UserServiceApi;
import com.strandls.user.pojo.User;
import com.strandls.user.pojo.UserIbp;

import net.minidev.json.JSONArray;

public class NakshaIntegratorServicesImpl implements NakshaIntegratorServices {

	@Inject
	private UserServiceApi userServiceApi;

	private final Logger logger = LoggerFactory.getLogger(NakshaIntegratorServicesImpl.class);

	private byte[] getRequest(String uri, List<NameValuePair> params) {

		CloseableHttpResponse response = null;
		CloseableHttpClient httpclient = null;
		byte[] byteArrayResponse = null;

		String host = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiHost");
		String portalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");

		try {

			// String url =
			// "https://staging.communityconservedareas.org/naksha-api/api/layer/all?limit=10&offset=0";

			URIBuilder builder = new URIBuilder();
			builder.setScheme("https").setHost(host).setPath(uri);

			if (params != null)
				builder.setParameters(params);
			URI Uri = null;
			Uri = builder.build();
			HttpGet request = new HttpGet(Uri);

			request.setHeader("Portal-Id", portalId);

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

				String authorId = metaLayer.get("uploaderUserId").toString();

				UserIbp userIbp = userServiceApi.getUserIbp(authorId + "");
				metaLayer.put("author", userIbp);

				// Boolean isDownloadable = checkDownLoadAccess(userProfile, metaLayer);

				layerLists.add(metaLayer);
			}
			return layerLists;

		} catch (Exception e) {
			logger.error(e.getMessage());
			return layerLists;
		}

	}

}
