package com.autotune.utils;

import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.experiments.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.String.valueOf;

public class TrialHelpers {
    /**
     * Convert the given ExperimentTrial object to JSON. This will be sent to the EM module
     *
     * @param experimentTrial object that holds the trial config
     * @return Equivalent JSONObject that is accepted by EM
     */
    public static JSONObject experimentTrialToJSON(ExperimentTrial experimentTrial) {

        /*
         * Top level experimentTrialJSON Object
         */
        JSONObject experimentTrialJSON = new JSONObject();
        experimentTrialJSON.put("experiment_id", experimentTrial.getExperimentId());
        experimentTrialJSON.put("namespace", experimentTrial.getNamespace());
        experimentTrialJSON.put("application_name", experimentTrial.getApplicationName());
        experimentTrialJSON.put("app-version", experimentTrial.getAppVersion());

        /*
         * Info object
         * experimentTrialJSON -> info
         */
        JSONObject infoValues = new JSONObject();
        infoValues.put("trial_id", experimentTrial.getTrialInfo().getTrialId());
        infoValues.put("trial_num", experimentTrial.getTrialInfo().getTrialNum());

        experimentTrialJSON.put("info", infoValues);

        /*
         * trialSettings object
         * experimentTrialJSON -> settings -> trail_settings
         */
        JSONObject trialSettingsValues = new JSONObject();
        trialSettingsValues.put("trial_run", experimentTrial.getExperimentSettings().getTrialSettings().getTrialRun());
        trialSettingsValues.put("trial_measurement_time", experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementTime());

        /*
         * deploymentPolicy object
         * experimentTrialJSON -> settings -> deployment_settings -> deployment_policy
         */
        JSONObject deploymentPolicyValues = new JSONObject();
        deploymentPolicyValues.put("type", experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentPolicy().getDeploymentType());
        deploymentPolicyValues.put("target_env", experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentPolicy().getTargetEnv());
        deploymentPolicyValues.put("agent", experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentPolicy().getAgent());

        /*
         * deployment_tracking object
         * experimentTrialJSON -> settings -> deployment_settings -> deployment_tracking
         */
        JSONArray trackersArray = new JSONArray();
        for (String trackerObjs : experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentTracking().getTrackers()) {
            trackersArray.put(trackerObjs);
        }
        JSONObject trackers = new JSONObject();
        trackers.put("trackers", trackersArray);

        JSONObject deploymentSettingsValues = new JSONObject();
        deploymentSettingsValues.put("deployment_policy", deploymentPolicyValues);
        deploymentSettingsValues.put("deployment_tracking", trackers);

        JSONObject experimentSettingsValues = new JSONObject();
        experimentSettingsValues.put("trial_settings", trialSettingsValues);
        experimentSettingsValues.put("deployment_settings", deploymentSettingsValues);
        // Update experimentTrialJSON
        experimentTrialJSON.put("settings", experimentSettingsValues);

        /*
         * deployments object section
         */
        JSONArray deploymentsArrayObjs = new JSONArray();
        for (Deployments deployment : experimentTrial.getDeployments()) {
            /*
             * resources object
             * experimentTrialJSON -> deployments -> config -> resources
             */
            JSONObject requestsValues = new JSONObject();
            /* CPU Value should only be */
            requestsValues.put("cpu", String.format("%.2f", deployment.getRequests().getCpuValue()) +
                    deployment.getRequests().getCpuUnits());
            requestsValues.put("memory", String.valueOf(deployment.getRequests().getMemoryValue()) +
                    deployment.getRequests().getMemoryUnits());
            JSONObject resourcesValues = new JSONObject();
            resourcesValues.put("requests", requestsValues);
            resourcesValues.put("limits", requestsValues);
            JSONObject resources = new JSONObject();
            resources.put("resources", resourcesValues);

            JSONObject container = new JSONObject();
            container.put("container", resources);

            JSONObject tspec = new JSONObject();
            tspec.put("spec", container);

            JSONObject template = new JSONObject();
            template.put("template", tspec);

            JSONObject resObject = new JSONObject();
            resObject.put("name", "update requests and limits");
            resObject.put("spec", template);

            /*
             * env object
             * experimentTrialJSON -> deployments -> config -> env
             */
            JSONObject envValues = new JSONObject();
            envValues.put("JVM_OPTIONS", deployment.getJvmOptions());
            envValues.put("JVM_ARGS", deployment.getJvmOptions());

            JSONObject env = new JSONObject();
            env.put("env", envValues);

            JSONObject containere = new JSONObject();
            containere.put("container", env);

            JSONObject tspece = new JSONObject();
            tspece.put("spec", containere);

            JSONObject templatee = new JSONObject();
            templatee.put("template", tspece);

            JSONObject envObject = new JSONObject();
            envObject.put("name", "update env");
            envObject.put("spec", templatee);

            /*
             * config object
             * experimentTrialJSON -> deployments -> config
             */
            JSONArray configArrayObjects = new JSONArray();
            configArrayObjects.put(resObject);
            configArrayObjects.put(envObject);

            /*
             * metrics object
             * experimentTrialJSON -> deployments -> metrics
             */
            JSONArray metricArrayObjects = new JSONArray();
            for (Metrics metricObjects : deployment.getMetrics()) {
                JSONObject obj = new JSONObject();
                obj.put("name", metricObjects.getName());
                obj.put("query", metricObjects.getQuery());
                obj.put("datasource", metricObjects.getDatasource());
                metricArrayObjects.put(obj);
            }

            JSONObject deploymentObject = new JSONObject();
            deploymentObject.put("type", deployment.getDeploymentType());
            deploymentObject.put("deployment_name", deployment.getDeploymentName());
            deploymentObject.put("namespace", deployment.getDeploymentNameSpace());
            deploymentObject.put("state", deployment.getState());
            deploymentObject.put("result", deployment.getResult());
            deploymentObject.put("result_info", deployment.getResultInfo());
            deploymentObject.put("result_error", deployment.getResultError());
            deploymentObject.put("metrics", metricArrayObjects);
            deploymentObject.put("config", configArrayObjects);

            // Add this deployment tracker object to the deployment object array
            deploymentsArrayObjs.put(deploymentObject);
        }
        experimentTrialJSON.put("deployments", deploymentsArrayObjs);

        return experimentTrialJSON;
    }

    /**
     * Create a ExperimentTrial object that holds the trial config to be deployed to the k8s cluster
     *
     * @param trialNumber passed in from Optuna
     * @param appSearchSpace from the user
     * @param trialConfigJson from Optuna
     * @return ExperimentTrial object
     */
    public static ExperimentTrial createDefaultExperimentTrial(int trialNumber,
                                                               ApplicationSearchSpace appSearchSpace,
                                                               String trialConfigJson) {

        TrialSettings trialSettings = new TrialSettings("15mins",
                "3mins");
        DeploymentPolicy deploymentPolicy = new DeploymentPolicy("rollingUpdate",
                "qa",
                "EM"
        );

        TrialInfo trialInfo = new TrialInfo("",
                trialNumber);

        ArrayList<String> trackers = new ArrayList<>();
        trackers.add("training");
        trackers.add("production");
        DeploymentTracking deploymentTracking = new DeploymentTracking(trackers);
        DeploymentSettings deploymentSettings = new DeploymentSettings(deploymentPolicy,
                deploymentTracking);
        ExperimentSettings experimentSettings = new ExperimentSettings(trialSettings,
                deploymentSettings);

        Resources requests = null, limits = null;
        String cpu = null, memory = null;

        ArrayList<Deployments> deployments = new ArrayList<>();
        for (String tracker : trackers ) {
            JSONArray trialConfigArray = new JSONArray(trialConfigJson);
            for (Object trialConfigObject : trialConfigArray) {
                JSONObject trialConfig = (JSONObject) trialConfigObject;
                if ("cpuRequest".equals(trialConfig.getString("tunable_name"))) {
                    cpu = valueOf(trialConfig.getDouble("tunable_value")) +
                            appSearchSpace.getApplicationTunablesMap().get("cpuRequest").getBoundUnits();
                    System.out.println("CPU Request: " + cpu);
                } else if ("memoryRequest".equals(trialConfig.getString("tunable_name"))) {
                    memory = valueOf(trialConfig.getDouble("tunable_value")) +
                            appSearchSpace.getApplicationTunablesMap().get("memoryRequest").getBoundUnits();
                    System.out.println("Mem Request: " + memory);
                }
            }
            if (cpu != null && memory != null) {
                requests = new Resources(cpu, memory);
                limits = new Resources(cpu, memory);
            }

            Metrics request_sum = new Metrics("request_sum", "request_sum_query", "prometheus");
            Metrics request_count = new Metrics("request_count", "request_count_query", "prometheus");
            Metrics hotspot_function = new Metrics("hotspot_function", "hotspot_function_query", "prometheus");
            Metrics cpuRequest = new Metrics("cpuRequest", "cpuRequest_query", "prometheus");
            Metrics memoryRequest = new Metrics("memoryRequest", "memoryRequest_query", "prometheus");
            ArrayList<Metrics> metrics = new ArrayList<>();
            metrics.add(request_sum);
            metrics.add(request_count);
            metrics.add(hotspot_function);
            metrics.add(cpuRequest);
            metrics.add(memoryRequest);

            Deployments deployment = new Deployments(tracker,
                    "petclinic-sample",
                    "default",
                    "",
                    "",
                    "",
                    "",
                    metrics,
                    requests,
                    limits,
                    "-XX:MaxInlineLevel=23"
            );
            deployments.add(deployment);
        }

        ExperimentTrial experimentTrial = new ExperimentTrial(appSearchSpace.getApplicationId(),
                "default",
                appSearchSpace.getApplicationName(),
                "v1",
                trialInfo,
                experimentSettings,
                deployments
        );

        return experimentTrial;
    }
}