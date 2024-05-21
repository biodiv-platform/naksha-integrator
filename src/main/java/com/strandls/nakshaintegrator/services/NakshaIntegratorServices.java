/**
 * 
 */
package com.strandls.nakshaintegrator.services;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.strandls.user.ApiException;

public interface NakshaIntegratorServices {
	public List<HashMap<String, Object>> getTOCList(HttpServletRequest request, Integer limit, Integer offset,
			boolean showOnlyPending);

}
