/**
 * 
 */
package com.strandls.nakshaintegrator.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.user.ApiException;

public interface NakshaIntegratorServices {
	public List<HashMap<String, Object>> getTOCList(HttpServletRequest request, Integer limit, Integer offset,
			boolean showOnlyPending);

	public Map<String, Object> uploadLayer(HttpServletRequest request, FormDataMultiPart multiPart);

	public Map<String, Object> getLayerInfo(String layer);

	public Map<String, Object> makeLayerPending(String layerName);

	public Map<String, Object> makeLayerActive(String layerName);

	public byte[] downloadShpFile(String hashKey, String layerName);

	public Map<String, Object> prepareDownloadLayer(HttpServletRequest request, Map<String, Object> layerDownload);

	public Map<String, Object> getLocationInfo(String lat, String lon);

}
