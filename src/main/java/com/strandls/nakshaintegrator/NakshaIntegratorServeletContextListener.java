/**
 * 
 */
package com.strandls.nakshaintegrator;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContextEvent;

import org.glassfish.jersey.servlet.ServletContainer;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.strandls.nakshaintegrator.controllers.NakshaIntegratorControllerModule;
import com.strandls.nakshaintegrator.dao.NakshaIntegratorDaoModule;
import com.strandls.nakshaintegrator.services.NakshaIntegratorServiceModule;
import com.strandls.user.controller.UserServiceApi;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class NakshaIntegratorServeletContextListener extends GuiceServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(NakshaIntegratorServeletContextListener.class);

	@Override
	protected Injector getInjector() {

		return Guice.createInjector(new ServletModule() {
			@Override
			protected void configureServlets() {

				Configuration configuration = new Configuration();

				try {
					for (Class<?> cls : getEntityClassesFromPackage("com")) {
						configuration.addAnnotatedClass(cls);
					}
				} catch (ClassNotFoundException | IOException | URISyntaxException e) {
					logger.error(e.getMessage());
				}

				configuration = configuration.configure();
				SessionFactory sessionFactory = configuration.buildSessionFactory();

//				Rabbit MQ initialisation: one long-lived Connection for the app;
//				channels are handed out per-thread via RabbitChannelProvider instead
//				of a single Channel being shared/injected everywhere.
				RabbitMqConnection rabbitMqConnection = new RabbitMqConnection();
				Connection rabbitConnection = null;
				try {
					rabbitConnection = rabbitMqConnection.connect();
				} catch (Exception e) {
					logger.error("Failed to establish RabbitMQ connection", e);
				}

				GeometryFactory geofactory = new GeometryFactory(new PrecisionModel(), 4326);
				bind(GeometryFactory.class).toInstance(geofactory);
				bind(SessionFactory.class).toInstance(sessionFactory);
				bind(Connection.class).toInstance(rabbitConnection);
				bind(RabbitChannelProvider.class).in(Scopes.SINGLETON);

				Map<String, String> props = new HashMap<>();
				props.put("jakarta.ws.rs.Application", ApplicationConfig.class.getName());
				props.put("jersey.config.server.provider.packages", "com");
				props.put("jersey.config.server.wadl.disableWadl", "true");

				bind(UserServiceApi.class).in(Scopes.SINGLETON);
				bind(ServletContainer.class).in(Scopes.SINGLETON);

				serve("/api/*").with(ServletContainer.class, props);
			}
		}, new NakshaIntegratorControllerModule(), new NakshaIntegratorServiceModule(),
				new NakshaIntegratorDaoModule());

	}

	protected List<Class<?>> getEntityClassesFromPackage(String packageName)
			throws URISyntaxException, IOException, ClassNotFoundException {

		List<String> classNames = ApplicationConfig.getClassNamesFromPackage(packageName);
		List<Class<?>> classes = new ArrayList<>();
		for (String className : classNames) {
			Class<?> cls = Class.forName(className);
			Annotation[] annotations = cls.getAnnotations();

			for (Annotation annotation : annotations) {
				if (annotation instanceof jakarta.persistence.Entity) {
					classes.add(cls);
				}
			}
		}

		return classes;
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {

		Injector injector = (Injector) servletContextEvent.getServletContext().getAttribute(Injector.class.getName());

		SessionFactory sessionFactory = injector.getInstance(SessionFactory.class);
		sessionFactory.close();

		Connection rabbitConnection = injector.getInstance(Connection.class);
		if (rabbitConnection != null) {
			// abort() (unlike close()) forces the connection down immediately and
			// cancels any in-flight/scheduled automatic-recovery attempt, and never
			// throws. A graceful close() was observed leaving the client's own
			// background recovery thread alive past contextDestroyed(), which then
			// crashed trying to use this webapp's classloader after Tomcat had
			// already stopped it (surfacing as a redeploy/reload memory leak).
			rabbitConnection.abort(AMQP.REPLY_SUCCESS, "context destroyed", 5000);
		}

		super.contextDestroyed(servletContextEvent);
		// ... First close any background tasks which may be using the DB ...
		// ... Then close any DB connection pools ...

		// Now deregister JDBC drivers in this context's ClassLoader:
		// Get the webapp's ClassLoader
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// Loop through all drivers
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == cl) {
				// This driver was registered by the webapp's ClassLoader, so deregister it:
				try {
					logger.info("Deregistering JDBC driver {}", driver);
					DriverManager.deregisterDriver(driver);
				} catch (SQLException ex) {
					logger.error("Error deregistering JDBC driver {}", driver, ex);
				}
			} else {
				// driver was not registered by the webapp's ClassLoader and may be in use
				// elsewhere
				logger.trace("Not deregistering JDBC driver {} as it does not belong to this webapp's ClassLoader",
						driver);
			}
		}

	}
}
