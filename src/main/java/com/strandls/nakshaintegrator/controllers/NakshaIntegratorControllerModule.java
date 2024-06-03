/**
 * 
 */
package com.strandls.nakshaintegrator.controllers;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * 
 * @author vilay
 *
 */
public class NakshaIntegratorControllerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(NakshaIntegratorController.class).in(Scopes.SINGLETON);
		bind(GeoserverIntegratorController.class).in(Scopes.SINGLETON);
	}
}
