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
package net.sf.appstatus.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import net.sf.appstatus.IStatusResult;
import net.sf.appstatus.StatusService;

public class StatusServlet extends HttpServlet {

	String allow = null;
	private static final long serialVersionUID = 3912325072098291029L;
	private static final String STATUS_OK = "ok";
	private static final String STATUS_ERROR = "error";
	private static final String STATUS_WARN = "warn";
	private static final String STATUS_PROP = "prop";

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			InputStream is = StatusServlet.class
					.getResourceAsStream("/status-web-conf.properties");

			Properties p = new Properties();
			p.load(is);

			is.close();
			allow = (String) p.get("ip.allow");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Serve icons
	 * 
	 * @param id
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	protected void doGetResource(String id, HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		String location = null;
		if ("ok".equals(id)) {
			location = "/org/freedesktop/tango/22x22/status/weather-clear.png";
		} else if ("warn".equals(id)) {
			location = "/org/freedesktop/tango/22x22/status/weather-overcast.png";
		} else if ("error".equals(id)) {
			location = "/org/freedesktop/tango/22x22/status/weather-severe-alert.png";
		} else if ("prop".equals(id)) {
			location = "/org/freedesktop/tango/22x22/actions/format-justify-fill.png";
		}

		InputStream is = this.getClass().getResourceAsStream(location);
		OutputStream os = resp.getOutputStream();
		IOUtils.copy(is, os);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (allow != null) {
			if (!req.getRemoteAddr().equals(allow)) {
				resp.sendError(401, "IP not authorized");
				return;
			}
		}

		if (req.getParameter("icon") != null) {

			doGetResource(req.getParameter("icon"), req, resp);
			return;
		}

		List<IStatusResult> results = StatusService.getInstance().checkAll();
		boolean statusOk = true;
		int statusCode = 200;
		for (IStatusResult r : results) {
			if (r.isFatal()) {
				resp.setStatus(500);
				statusCode = 500;
				statusOk = false;
				break;
			}
		}

		ServletOutputStream os = resp.getOutputStream();
		os.write("<h1>Status Page</h1>".getBytes());
		os.write(("<p>Online:" + statusOk + "</p>").getBytes());
		os.write(("<p>Code:" + statusCode + "</p>").getBytes());

		os.write("<h2>Status</h2>".getBytes());
		os.write("<table border='1'>".getBytes());
		os
				.write("<tr><td></td><td>Name</td><td>Description</td><td>Code</td><td>Resolution</td></tr>"
						.getBytes());

		for (IStatusResult r : results) {
			generateRow(os, getStatus(r), r.getProbeName(), r.getDescription(),
					String.valueOf(r.getCode()), r.getResolutionSteps());
		}
		os.write("</table>".getBytes());

		os.write("<h2>Properties</h2>".getBytes());
		Map<String, String> properties = StatusService.getInstance()
				.getProperties();
		os.write("<table border='1'>".getBytes());
		os.write("<tr><td></td><td>Name</td><td>Value</td></tr>".getBytes());

		for (Entry<String, String> r : properties.entrySet()) {
			generateRow(os, "prop", r.getKey(), r.getValue());
		}
		os.write("</table>".getBytes());
	}

	/**
	 * Returns status icon id.
	 * 
	 * @param result
	 * @return
	 */
	private String getStatus(IStatusResult result) {

		if (result.isFatal())
			return "error";

		if (result.getCode() == IStatusResult.OK)
			return "ok";

		return "warn";
	}

	/**
	 * Outputs one table row
	 * 
	 * @param os
	 * @param status
	 * @param cols
	 * @throws IOException
	 */
	private void generateRow(ServletOutputStream os, String status,
			Object... cols) throws IOException {
		os.write("<tr>".getBytes());

		os.write(("<td><img src='?icon=" + status + "'></td>").getBytes());

		for (Object obj : cols) {
			os.write("<td>".getBytes());
			if (obj != null) {
				os.write(obj.toString().getBytes());
			}
			os.write("</td>".getBytes());

		}
		os.write("</tr>".getBytes());
	}

}
