/**
 * 
 */
package com.strandls.nakshaintegrator.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.user.ApiException;

public interface NakshaIntegratorServices {
	public List<HashMap<String, Object>> getTOCList(HttpServletRequest request, Integer limit, Integer offset,
			boolean showOnlyPending);

	public Map<String, Object> uploadLayer(HttpServletRequest request, FormDataMultiPart multiPart);

}
