/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.services;

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.datasource.DataSource;
import com.autotune.analyzer.datasource.DataSourceFactory;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.deployment.DeploymentInfo;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.k8sObjects.Metric;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class ListAppTunables extends HttpServlet
{
	/**
	 * Returns the list of applications monitored by autotune along with their tunables
	 *
	 * Request:
	 * `GET /listAppTunables` gives the tunables and layer information for all the applications monitored by autotune.
	 *
	 * `GET /listAppTunables?experiment_name=<EXPERIMENT_NAME>` for getting the tunables information of a specific application.
	 *
	 * `GET /listAppTunables?application_name=<EXPERIMENT_NAME>&layer_name='<LAYER>' for getting tunables of a specific layer for the application.
	 *
	 * Example JSON:
	 * {
	 *       "experiment_name": "app1_autotune",
	 *       “objective_function”: “transaction_response_time”,
	 *       "slo_class": "response_time",
	 *       “direction”: “minimize”
	 *       "layers": [
	 *         {
	 *           "layer_level": 0,
	 *           "layer_name": "container",
	 *           "layer_details": "generic container tunables"
	 *           "tunables": [
	 *             {
	 *                 "name": "memoryRequest",
	 *                 "upper_bound": "300M",
	 *                 "lower_bound": "150M",
	 *                 "value_type": "double",
	 *                 "query_url": "http://prometheus:9090/container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}"
	 *             },
	 *             {
	 *                 "name": "cpuRequest",
	 *                 "upper_bound": "3.0",
	 *                 "lower_bound": "1.0",
	 *                 "value_type": "double",
	 *                 "query_url": "http://prometheus:9090/(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])"
	 *             }
	 *           ]
	 *         },
	 *         {
	 *           "layer_level": 1,
	 *           "layer_name": "openj9",
	 *           "layer_details": "java openj9 tunables",
	 *           "tunables": [
	 *             {
	 *               "name": "javaHeap",
	 *               "upper_bound": "250M",
	 *               "lower_bound": "100M",
	 *               "value_type": "double",
	 *               "query_url": "http://prometheus:9090/jvm_memory_used_bytes{area=\"heap\",id=\"nursery-allocate\", pod=petclinic-deployment-6d4c8678d4-jmz8x}"
	 *             }
	 *           ]
	 *       }
	 *     ]
	 *   }
	 * ]
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String podName = req.getParameter(AnalyzerConstants.ServiceConstants.POD_NAME);
		String layerName = req.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);

		// If no autotuneobjects in the cluster
		if (AutotuneDeployment.autotuneObjectMap.isEmpty()) {
			outputJsonArray.put("Error: No objects of kind Autotune found!");
		} else {
			//If no pod_name was passed in the request, print all apps in all autotune objects in response
			if (podName == null) {
				for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
					AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);
					for (String application : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
						addAppTunablesToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, application, layerName);
					}
				}
			} else {
				boolean doesApplicationExist = false;
				for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
					if (AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).containsKey(podName)) {
						AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);
						addAppTunablesToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, podName, layerName);
						doesApplicationExist = true;
					}
				}

				//If no such application is monitored by autotune
				if (!doesApplicationExist) {
					outputJsonArray.put("Error: Application " + podName + " not found!");
				}
			}
		}
		resp.getWriter().println(outputJsonArray.toString(4));
	}

	private void addAppTunablesToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String podName, String layerName) {

		ApplicationServiceStack applicationServiceStack = AutotuneDeployment.applicationServiceStackMap.get(autotuneObject.getExperimentName()).get(podName);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, autotuneObject.getExperimentName());
		jsonObject.put(AnalyzerConstants.ServiceConstants.POD_NAME, podName);
		jsonObject.put(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME, applicationServiceStack.getDeploymentName());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE, autotuneObject.getNamespace());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSloInfo().getDirection());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSloInfo().getObjectiveFunction());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, autotuneObject.getSloInfo().getSloClass());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.ID, autotuneObject.getExperimentId());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, autotuneObject.getSloInfo().getHpoAlgoImpl());

		// Add function_variables info
		JSONArray functionVariablesArray = new JSONArray();
		for (Metric metric : autotuneObject.getSloInfo().getFunctionVariables()) {
			JSONObject functionVariableJson = new JSONObject();
			functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.NAME, metric.getName());
			functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE, metric.getValueType());
			try {
				final DataSource dataSource = DataSourceFactory.getDataSource(DeploymentInfo.getMonitoringAgent());
				functionVariableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, Objects.requireNonNull(dataSource).getDataSourceURL() +
						dataSource.getQueryEndpoint() + metric.getQuery());
			} catch (MonitoringAgentNotFoundException e) {
				functionVariableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, metric.getQuery());
			}

			functionVariablesArray.put(functionVariableJson);
		}

		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES, functionVariablesArray);

		JSONArray layersArray = new JSONArray();
		for (String autotuneConfigName : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey)
				.get(podName).getApplicationServiceStackLayers().keySet()) {
			AutotuneConfig autotuneConfig = AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey)
					.get(podName).getApplicationServiceStackLayers().get(autotuneConfigName);
			if (layerName == null || autotuneConfigName.equals(layerName)) {
				JSONObject layerJson = new JSONObject();
				layerJson.put(AnalyzerConstants.AutotuneConfigConstants.ID, autotuneConfig.getLayerId());
				layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, autotuneConfig.getLayerName());
				layerJson.put(AnalyzerConstants.ServiceConstants.LAYER_DETAILS, autotuneConfig.getDetails());
				layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, autotuneConfig.getLevel());

				JSONArray tunablesArray = new JSONArray();
				for (Tunable tunable : autotuneConfig.getTunables()) {
					JSONObject tunableJson = new JSONObject();
					String tunableQuery = tunable.getQueries().get(DeploymentInfo.getMonitoringAgent());
					tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getName());
					tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
					tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
					tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());
					tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());
					try {
						String query = AnalyzerConstants.NONE;
						final DataSource dataSource = DataSourceFactory.getDataSource(DeploymentInfo.getMonitoringAgent());
						// If tunable has a query specified
						if (tunableQuery != null && !tunableQuery.isEmpty()) {
							query = Objects.requireNonNull(dataSource).getDataSourceURL() +
									dataSource.getQueryEndpoint() + tunableQuery;
						}
						tunableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, query);
					} catch (MonitoringAgentNotFoundException e) {
						tunableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, tunableQuery);
					}
					tunablesArray.put(tunableJson);
				}
				layerJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesArray);
				layersArray.put(layerJson);
			}
		}
		if (layersArray.isEmpty()) {
			// No autotuneconfig objects currently being monitored.
			if (layerName == null)
				outputJsonArray.put("Error: No AutotuneConfig objects found!");
			else
				outputJsonArray.put("Error: AutotuneConfig " + layerName + " not found!");
			return;
		}
		jsonObject.put(AnalyzerConstants.ServiceConstants.LAYERS, layersArray);
		outputJsonArray.put(jsonObject);
	}
}
