package com.strandls.nakshaintegrator.util;

import com.google.inject.Singleton;
import com.strandls.authentication_utility.util.PropertyFileUtil;

import me.desair.tus.server.TusFileUploadService;

@Singleton
public class TusConfig {

	private final TusFileUploadService tusFileUploadService;

	public TusConfig() {
		String storagePath = PropertyFileUtil.fetchProperty("config.properties", "layerTusUploadPath");
		this.tusFileUploadService = new TusFileUploadService().withStoragePath(storagePath)
				.withMaxUploadSize(2L * 1024 * 1024 * 1024).withUploadExpirationPeriod(24 * 60 * 60 * 1000L);
	}

	public TusFileUploadService getService() {
		return tusFileUploadService;
	}
}