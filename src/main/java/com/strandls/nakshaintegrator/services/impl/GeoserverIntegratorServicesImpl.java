package com.strandls.nakshaintegrator.services.impl;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.strandls.authentication_utility.util.PropertyFileUtil;
import com.strandls.nakshaintegrator.services.GeoserverIntegratorServices;
import com.strandls.nakshaintegrator.services.impl.NakshaIntegratorServicesImpl;
import com.strandls.user.controller.UserServiceApi;

public class GeoserverIntegratorServicesImpl implements GeoserverIntegratorServices {

	@Inject
	private UserServiceApi userServiceApi;

	private final Logger logger = LoggerFactory.getLogger(NakshaIntegratorServicesImpl.class);

	private byte[] getRequest(String uri, List<NameValuePair> params) {

		CloseableHttpResponse response = null;
		CloseableHttpClient httpclient = null;
		byte[] byteArrayResponse = null;

		String host = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiHost");
		String portalId = PropertyFileUtil.fetchProperty("config.properties", "portalId");
		String apikey = PropertyFileUtil.fetchProperty("config.properties", "nakshaApiKey");

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
	public String getStyles(String workspace, String id) {
		String uri = "/naksha-api/api/geoserver/workspaces/" + workspace + "/styles/" + id;
		byte[] ans = getRequest(uri, null);
		String result = new String(ans);
		return result;
	}
}
