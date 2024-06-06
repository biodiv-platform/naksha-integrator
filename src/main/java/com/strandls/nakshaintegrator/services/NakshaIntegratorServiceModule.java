package com.strandls.nakshaintegrator.services;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import com.strandls.nakshaintegrator.services.impl.GeoserverIntegratorServicesImpl;
import com.strandls.nakshaintegrator.services.impl.MailServiceImpl;
import com.strandls.nakshaintegrator.services.impl.NakshaIntegratorServicesImpl;

public class NakshaIntegratorServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(NakshaIntegratorServices.class).to(NakshaIntegratorServicesImpl.class).in(Scopes.SINGLETON);
		bind(GeoserverIntegratorServices.class).to(GeoserverIntegratorServicesImpl.class).in(Scopes.SINGLETON);
		bind(MailService.class).to(MailServiceImpl.class).in(Scopes.SINGLETON);
	}
}
