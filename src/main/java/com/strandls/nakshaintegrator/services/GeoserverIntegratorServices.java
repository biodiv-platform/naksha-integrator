package com.strandls.nakshaintegrator.services;

import com.strandls.nakshaintegrator.pojo.GeoServerResponse;

public interface GeoserverIntegratorServices {
	public String getStyles(String workspace, String id);

	public GeoServerResponse getTyles(String layer, String z, String y, String x);

	public byte[] getThumbnails(String layer, String workspace, String bbox, String width, String height, String srs);

	public byte[] getPng(String bbox, String width, String height, String srs, String layers);
}
