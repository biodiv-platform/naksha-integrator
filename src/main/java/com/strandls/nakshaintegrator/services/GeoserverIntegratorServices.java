package com.strandls.nakshaintegrator.services;

public interface GeoserverIntegratorServices {
	public String getStyles(String workspace, String id);

	public byte[] getTyles(String layer, String z, String y, String x);
}
