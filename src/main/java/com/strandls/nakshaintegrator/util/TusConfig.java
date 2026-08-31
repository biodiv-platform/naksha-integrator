package com.strandls.nakshaintegrator.util;

import com.google.inject.Singleton;
import me.desair.tus.server.TusFileUploadService;
import com.strandls.authentication_utility.util.PropertyFileUtil;

@Singleton
public class TusConfig {

    private final TusFileUploadService tusFileUploadService;

    public TusConfig() {
        String storagePath = PropertyFileUtil.fetchProperty("config.properties", "tus");
        this.tusFileUploadService = new TusFileUploadService()
                .withStoragePath(storagePath)
                .withMaxUploadSize(2L * 1024 * 1024 * 1024)
                .withUploadExpirationPeriod(24 * 60 * 60 * 1000L); // tus's own cleanup for abandoned/incomplete uploads
    }

    public TusFileUploadService getService() {
        return tusFileUploadService;
    }
}