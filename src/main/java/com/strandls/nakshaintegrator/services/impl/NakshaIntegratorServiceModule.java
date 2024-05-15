/**
 * 
 */
package com.strandls.nakshaintegrator.services.impl;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.strandls.nakshaintegrator.services.NakshaIntegratorServices;

/**
 * 
 * @author vilay
 *
 */
public class NakshaIntegratorServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(NakshaIntegratorServices.class).to(NakshaIntegratorServicesImpl.class).in(Scopes.SINGLETON);
	}
}
