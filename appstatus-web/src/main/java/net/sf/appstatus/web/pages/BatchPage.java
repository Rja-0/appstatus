/*
 * Copyright 2010-2013 Capgemini Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */package net.sf.appstatus.web.pages;

import static java.lang.Math.round;
import static net.sf.appstatus.web.HtmlUtils.applyLayout;
import static net.sf.appstatus.web.HtmlUtils.countAndDetail;
import static net.sf.appstatus.web.HtmlUtils.generateBeginTable;
import static net.sf.appstatus.web.HtmlUtils.generateEndTable;
import static net.sf.appstatus.web.HtmlUtils.generateHeaders;
import static net.sf.appstatus.web.HtmlUtils.generateRow;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.text.StrBuilder;

import net.sf.appstatus.core.batch.IBatch;
import net.sf.appstatus.core.batch.IBatchConfiguration;
import net.sf.appstatus.core.batch.IBatchManager;
import net.sf.appstatus.core.batch.IBatchScheduleManager;
import net.sf.appstatus.web.HtmlUtils;
import net.sf.appstatus.web.StatusWebHandler;

public class BatchPage extends AbstractPage {
	private static final String CLEAR_ITEM = "clear-item";
	private static final String CLEAR_OLD = "clear-old";
	private static final String CLEAR_SUCCESS = "clear-success";
	private static final String ENCODING = "UTF-8";
	private static final String ITEM_UUID = "item-uuid";
	private static final String PAGECONTENTLAYOUT = "batchesContentLayout.html";

	/**
	 * Adding execution expressions informations from a list of IBatch.
	 *
	 * @param conf
	 * @param listBatchs
	 */
	private void addExecutionInformations(IBatchConfiguration conf, List<? extends IBatch> listBatchs) {

		if (isEmpty(listBatchs)) {
			return;
		}

		IBatch latestExecution = null;

		for (IBatch batch : listBatchs) {
			if (latestExecution == null || latestExecution.getStartDate().before(batch.getEndDate())) {
				latestExecution = batch;
			}

		}
		conf.setLastExecution(latestExecution.getStartDate());
	}

	@Override
	public void doGet(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp)
			throws UnsupportedEncodingException, IOException {

		setup(resp, "text/html");
		ServletOutputStream os = resp.getOutputStream();
		Map<String, String> valuesMap = new HashMap<String, String>();
		StrBuilder sbRunningBatchesBatchesTable = new StrBuilder();
		StrBuilder sbFinishedBatchesBatchesTable = new StrBuilder();
		StrBuilder sbErrorsBatchesBatchesTable = new StrBuilder();
		StrBuilder sbConfBatchesBatchesTable = new StrBuilder();

		IBatchManager manager = webHandler.getAppStatus().getBatchManager();
		List<IBatch> runningBatches = manager.getRunningBatches();
		if (generateBeginTable(sbRunningBatchesBatchesTable, runningBatches.size())) {

			generateHeaders(sbRunningBatchesBatchesTable, "", "Id", "Group", "Name", "Start", "Progress", //
					"End (est.)", "Status", "Task", "Last Msg", "Items", "Rejected", "Last Update");

			for (IBatch batch : runningBatches) {
				generateRow(sbRunningBatchesBatchesTable, getIcon(batch), generateId(resp, batch.getUuid()), //
						escapeHtml4(batch.getGroup()), escapeHtml4(batch.getName()), batch.getStartDate(),
						getProgressBar(batch), //
						batch.getEndDate(), batch.getStatus(), batch.getCurrentTask(), //
						escapeHtml4(batch.getLastMessage()), batch.getItemCount(),
						HtmlUtils.countAndDetail(batch.getRejectedItemsId()), //
						batch.getLastUpdate(), //
						batch.getStatus() != IBatch.STATUS_ZOMBIE ? "" //
								: "<form action='?p=batch' method='post'><input type='submit' name='" + CLEAR_ITEM //
										+ "' value='Delete'  class='btn btn-small' /><input type=hidden name='" //
										+ ITEM_UUID + "' value='" + escapeHtml4(batch.getUuid()) + "'/></form>");
			}

			generateEndTable(sbRunningBatchesBatchesTable, runningBatches.size());
		}

		// Batch Schedule
		List<IBatchConfiguration> batchConfigurations = new ArrayList<IBatchConfiguration>();

		IBatchScheduleManager scheduleManager = webHandler.getAppStatus().getBatchScheduleManager();
		if (scheduleManager != null) {
			batchConfigurations.addAll(scheduleManager.getBatchConfigurations());
		}

		if (HtmlUtils.generateBeginTable(sbConfBatchesBatchesTable, batchConfigurations.size())) {
			HtmlUtils.generateHeaders(sbConfBatchesBatchesTable, "", "Group", "Name", "Last run", "Next", "Exec. expr");

			for (IBatchConfiguration batch : batchConfigurations) {
				addExecutionInformations(batch, manager.getBatches(batch.getGroup(), batch.getName()));
				generateRow(sbConfBatchesBatchesTable, Resources.STATUS_JOB, escapeHtml4(batch.getGroup()),
						escapeHtml4(batch.getName()), batch.getLastExecution(), batch.getNextExecution(),
						batch.getSchedule());
			}

			generateEndTable(sbConfBatchesBatchesTable, runningBatches.size());
		}

		List<IBatch> finishedBatches = manager.getFinishedBatches();

		if (generateBeginTable(sbFinishedBatchesBatchesTable, finishedBatches.size())) {

			generateHeaders(sbFinishedBatchesBatchesTable, "", "Id", "Group", "Name", "Start", "Progress", //
					"End", "Status", "Task", "Last Msg", "Items", "Rejected", "Last Update", "");
			for (IBatch batch : finishedBatches) {
				generateRow(sbFinishedBatchesBatchesTable, getIcon(batch), generateId(resp, batch.getUuid()), //
						escapeHtml4(batch.getGroup()), escapeHtml4(batch.getName()), batch.getStartDate(),
						getProgressBar(batch), //
						batch.getEndDate(), batch.getStatus(), batch.getCurrentTask(),
						escapeHtml4(batch.getLastMessage()), //
						batch.getItemCount(), countAndDetail(batch.getRejectedItemsId()), //
						batch.getLastUpdate(), //
						"<form action='?p=batch' method='post'><input type='submit' name='" //
								+ CLEAR_ITEM + "' value='Delete'  class='btn btn-small' /><input type=hidden name='" //
								+ ITEM_UUID + "' value='" + escapeHtml4(batch.getUuid()) + "'/></form>");
			}

			generateEndTable(sbFinishedBatchesBatchesTable, finishedBatches.size());
		}

		List<IBatch> errorBatches = manager.getErrorBatches();

		if (generateBeginTable(sbErrorsBatchesBatchesTable, errorBatches.size())) {

			generateHeaders(sbErrorsBatchesBatchesTable, "", "Id", "Group", "Name", "Start", "Progress", //
					"End", "Status", "Task", "Last Msg", "Items", "Rejected", "Last Update", "");

			for (IBatch batch : errorBatches) {
				generateRow(sbErrorsBatchesBatchesTable, getIcon(batch), generateId(resp, batch.getUuid()), //
						escapeHtml4(batch.getGroup()), escapeHtml4(batch.getName()), batch.getStartDate(),
						getProgressBar(batch), //
						batch.getEndDate(), batch.getStatus(), batch.getCurrentTask(),
						escapeHtml4(batch.getLastMessage()), //
						batch.getItemCount(), HtmlUtils.countAndDetail(batch.getRejectedItemsId()), //
						batch.getLastUpdate(), //
						"<form action='?p=batch' method='post'><input type='submit' name='" //
								+ CLEAR_ITEM + "' value='Delete' class='btn btn-small'/><input type=hidden name='" //
								+ ITEM_UUID + "' value='" + escapeHtml4(batch.getUuid()) + "'/></form>");
			}
			generateEndTable(sbErrorsBatchesBatchesTable, errorBatches.size());
		}

		valuesMap.put("confBatchesBatchesTable", sbConfBatchesBatchesTable.toString());
		valuesMap.put("runningBatchesBatchesTable", sbRunningBatchesBatchesTable.toString());
		valuesMap.put("finishedBatchesBatchesTable", sbFinishedBatchesBatchesTable.toString());
		valuesMap.put("errorsBatchesBatchesTable", sbErrorsBatchesBatchesTable.toString());
		valuesMap.put("clearActions", generateClearActions());

		String content = applyLayout(valuesMap, PAGECONTENTLAYOUT);

		valuesMap.clear();
		valuesMap.put("content", content);

		os.write(getPage(webHandler, valuesMap).getBytes(ENCODING));
	}

	@Override
	public void doPost(StatusWebHandler webHandler, HttpServletRequest req, HttpServletResponse resp) {
		if (req.getParameter(CLEAR_OLD) != null) {
			webHandler.getAppStatus().getBatchManager().removeAllBatches(IBatchManager.REMOVE_OLD);
		} else if (req.getParameter(CLEAR_SUCCESS) != null) {
			webHandler.getAppStatus().getBatchManager().removeAllBatches(IBatchManager.REMOVE_SUCCESS);
		} else if (req.getParameter(CLEAR_ITEM) != null) {
			webHandler.getAppStatus().getBatchManager().removeBatch(req.getParameter(ITEM_UUID));
		}

	}

	private String generateClearActions() throws IOException {
		StrBuilder sb = new StrBuilder();
		sb.append("<p>Actions :</p><form action='?p=batch' method='post'><input type='submit' name='" + CLEAR_OLD //
				+ "' value='Delete old (6 months)' class='btn'/> <input type='submit' name='" + CLEAR_SUCCESS //
				+ "' value='Delete Success w/o rejected' class='btn'/></form>");
		return sb.toString();
	}

	private String generateId(HttpServletResponse resp, String id) throws IOException {

		if (id == null) {
			return "";
		}

		if (id.length() < 15) {
			return id;
		} else {
			return "<span title='" + escapeHtml4(id) + "'>" + escapeHtml4(id.substring(0, 10)) + "...</span";
		}

	}

	private String getIcon(IBatch b) {

		if (IBatch.STATUS_FAILURE.equals(b.getStatus())) {
			return Resources.STATUS_JOB_ERROR;
		}

		if (IBatch.STATUS_SUCCESS.equals(b.getStatus()) && b.getRejectedItemsId() != null //
				&& b.getRejectedItemsId().size() > 0) {
			return Resources.STATUS_JOB_WARNING;
		}

		return Resources.STATUS_JOB;

	}

	@Override
	public String getId() {
		return "batch";
	}

	@Override
	public String getName() {
		return "Batch";
	}

	String getProgressBar(IBatch batch) {

		String color = "success";
		if (batch.getRejectedItemsId() != null && batch.getRejectedItemsId().size() > 0) {
			color = "warning";
		}

		if (IBatch.STATUS_ZOMBIE.equals(batch.getStatus())) {
			color = "warning";
		}

		if (IBatch.STATUS_FAILURE.equals(batch.getStatus())) {
			color = "danger";
		}

		int percent = round(batch.getProgressStatus());

		String status = "progress-" + color;
		String active = "active";
		String striped = "progress-striped";
		// progress is animated if job is running (not complete and still
		// updated)
		if (IBatch.STATUS_ZOMBIE.equals(batch.getStatus()) || IBatch.STATUS_FAILURE.equals(batch.getStatus()) //
				|| IBatch.STATUS_SUCCESS.equals(batch.getStatus())) {
			active = "";
		}

		if (IBatch.STATUS_FAILURE.equals(batch.getStatus()) || IBatch.STATUS_SUCCESS.equals(batch.getStatus())) {
			striped = "";
		}

		if (percent == -1) {
			percent = 100;
		}

		return "<div class=\"progress " + striped + " " + active + " " + status + "\">" //
				+ "<div class=\"bar\" style=\"width: " + percent + "%;\"></div>" + "</div>";
	}
}
