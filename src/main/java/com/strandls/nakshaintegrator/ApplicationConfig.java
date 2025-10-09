/**
 *
 */
package com.strandls.nakshaintegrator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(info = @Info(title = "Naksha Integrator MicroServices", version = "1.0.0", description = "API for Naksha Integrator"), servers = {
		@Server(url = "http://localhost:8080/nakshaIntegrator-api/api") })
public class ApplicationConfig extends Application {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

	/**
	 *
	 */
	public ApplicationConfig() {
		logger.info("Initializing ApplicationConfig...");
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> singletons = new HashSet<>();

		// Lifecycle listener to bridge Guice & HK2
		singletons.add(new ContainerLifecycleListener() {
			@Override
			public void onStartup(Container container) {
				ServletContainer servletContainer = (ServletContainer) container;
				ServiceLocator serviceLocator = container.getApplicationHandler().getInjectionManager()
						.getInstance(ServiceLocator.class);
				GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
				GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
				Injector injector = (Injector) servletContainer.getServletContext()
						.getAttribute(Injector.class.getName());
				guiceBridge.bridgeGuiceInjector(injector);
			}

			@Override
			public void onShutdown(Container container) {
			}

			@Override
			public void onReload(Container container) {
			}
		});

		// Add OpenAPI resource
		singletons.add(new OpenApiResource());

		return singletons;
	}

	public static List<String> getClassNamesFromPackage(final String packageName)
			throws URISyntaxException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ArrayList<String> names = new ArrayList<>();
		URL packageURL = classLoader.getResource(packageName);

		URI uri = new URI(packageURL.toString());
		File folder = new File(uri.getPath());

		try (Stream<Path> files = Files.find(Paths.get(folder.getAbsolutePath()), 999,
				(p, bfa) -> bfa.isRegularFile())) {
			files.forEach(file -> {
				String name = file.toFile().getAbsolutePath()
						.replaceAll(folder.getAbsolutePath() + File.separatorChar, "").replace(File.separatorChar, '.');
				if (name.indexOf('.') != -1) {
					name = packageName + '.' + name.substring(0, name.lastIndexOf('.'));
					names.add(name);
				}
			});
		}

		return names;
	}
}
