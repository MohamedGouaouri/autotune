package com.autotune.analyzer.services;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.Experimentator;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.Tunable;
import com.autotune.utils.TrialHelpers;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListExperiments extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			JSONArray experimentTrialJSONArray = new JSONArray();
			for (String applicationID : Experimentator.experimentsMap.keySet()) {
				AutotuneExperiment autotuneExperiment = Experimentator.experimentsMap.get(applicationID);
				for (ExperimentTrial experimentTrial : autotuneExperiment.getExperimentTrials()) {
					JSONObject experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);
					experimentTrialJSONArray.put(experimentTrialJSON);
				}
			}
			response.getWriter().println(experimentTrialJSONArray.toString(4));
			response.getWriter().close();
		} catch (Exception ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void getExperiment(JSONArray outputJsonArray, String id) {
		if (id == null) {
			//No application parameter, generate search space for all applications
			for (String applicationID : Experimentator.applicationSearchSpaceMap.keySet()) {
				addExperiment(outputJsonArray, applicationID);
			}
		} else {
			if (Experimentator.applicationSearchSpaceMap.containsKey(id)) {
				addExperiment(outputJsonArray, id);
			}
		}

		if (outputJsonArray.isEmpty()) {
			if (AutotuneDeployment.autotuneObjectMap.isEmpty())
				outputJsonArray.put("Error: No objects of kind Autotune found!");
			else
				outputJsonArray.put("Error: Application " + id + " not found!");
		}
	}

	private void addExperiment(JSONArray outputJsonArray, String id) {
		JSONObject jsonObject = new JSONObject();
		ApplicationSearchSpace applicationSearchSpace = Experimentator.applicationSearchSpaceMap.get(id);

		String applicationID = applicationSearchSpace.getApplicationId();;
		String name = applicationSearchSpace.getApplicationName();

		//TODO Replace trialNum hardcoding
		int trialNum = 1;

		jsonObject.put("id", applicationID);
		jsonObject.put("application_name", name);
		jsonObject.put("trial_num", trialNum);

		JSONArray updateConfigJson = new JSONArray();

		for (String tunableName : Experimentator.tunablesMap.get(id).keySet()) {
			JSONObject tunableJson = new JSONObject();
			tunableJson.put("tunable_name", tunableName);
			tunableJson.put("tunable_value", Experimentator.tunablesMap.get(id).get(tunableName));
			updateConfigJson.put(tunableJson);
		}

		JSONArray queriesJsonArray = new JSONArray();
		for (String applicationTunableName : applicationSearchSpace.getApplicationTunablesMap().keySet()) {
			Tunable applicationTunable = applicationSearchSpace.getApplicationTunablesMap().get(applicationTunableName);
			JSONObject queryJson = new JSONObject();
			queryJson.put("tunable_name", applicationTunable.getName());
			queriesJsonArray.put(queryJson);
		}

		jsonObject.put("update_config", updateConfigJson);
		jsonObject.put("queries", queriesJsonArray);
		outputJsonArray.put(jsonObject);
	}
}