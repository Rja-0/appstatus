/*
 * Copyright 2010 Capgemini
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 */
package net.sf.appstatus;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusService {
	private static Logger logger = LoggerFactory.getLogger(StatusService.class);
	private static final String CONFIG_LOCATION = "status-check.properties";

	static StatusService instance = new StatusService();
	static List<IStatusChecker> probes;
	static List<IPropertyProvider> propertyProviders;

	public static StatusService getInstance() {
		return instance;
	}

	public StatusService() {
		probes = new ArrayList<IStatusChecker>();
		propertyProviders = new ArrayList<IPropertyProvider>();

		try {
			// Load and init all probes
			Enumeration<URL> probesURLs;

			probesURLs = StatusService.class.getClassLoader().getResources(
					CONFIG_LOCATION);

			URL url = null;
			Properties p = null;
			InputStream is = null;
			while (probesURLs.hasMoreElements()) {
				url = probesURLs.nextElement();

				// Load plugin configuration
				p = new Properties();
				is = url.openStream();
				p.load(is);
				is.close();

				Set<String> keys = p.stringPropertyNames();
				for (String name : keys) {
					if (name.startsWith("check")) {
						String clazz = (String) p.get(name);
						IStatusChecker check = (IStatusChecker) Class.forName(
								clazz).newInstance();
						probes.add(check);
						logger.info("Registered status checker "+clazz);
					} else if (name.startsWith("property")) {
						String clazz = (String) p.get(name);
						IPropertyProvider provider = (IPropertyProvider) Class
								.forName(clazz).newInstance();
						propertyProviders.add(provider);
						logger.info("Registered property provider "+clazz);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Initialization error", e);
		}

	}

	public List<IStatusResult> checkAll() {

		ArrayList<IStatusResult> l = new ArrayList<IStatusResult>();

		for (IStatusChecker check : probes) {
			l.add(check.checkStatus());
		}
		return l;

	}

	public Map<String, String> getProperties() {

		TreeMap<String, String> l = new TreeMap<String, String>();

		for (IPropertyProvider provider : propertyProviders) {
			l.putAll(provider.getProperties());
		}
		return l;
	}
}
