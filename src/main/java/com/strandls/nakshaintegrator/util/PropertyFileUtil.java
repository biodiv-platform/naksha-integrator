package com.strandls.nakshaintegrator.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFileUtil {
	/**
	 * since all the class members are static, we need to add a private static
	 * constructor
	 */
	private PropertyFileUtil() {
		super();
	}

	private static final Logger logger = LoggerFactory.getLogger(PropertyFileUtil.class);

	public static String fetchProperty(String fileName, String propertyName) {
		Properties properties = new Properties();
		String result = "";
		try {
			ClassLoader classLoader = PropertyFileUtil.class.getClassLoader();
			properties.load(classLoader.getResourceAsStream(fileName));
			result = properties.getProperty(propertyName);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return result;
	}
}
