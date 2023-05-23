/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.TrialHelpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.experiment.Experimentator.experimentsMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.*;

/**
 * Rest API used to list experiments.
 */
public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);
    KubernetesServices kubernetesServices = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String results = request.getParameter(KruizeConstants.JSONKeys.RESULTS);
        String latest = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String recommendations = request.getParameter(KruizeConstants.JSONKeys.RECOMMENDATIONS);
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();

        results = (results == null || results.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: results;
        recommendations = (recommendations == null || recommendations.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: recommendations;
        latest = (latest == null || latest.isEmpty()) ? AnalyzerConstants.BooleanString.TRUE: latest;

        String gsonStr;
        Gson gsonObj = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass() == Metric.class && (
                            f.getName().equals("trialSummaryResult")
                                    || f.getName().equals("cycleDataMap")
                    ) ||
                            f.getDeclaringClass() == ContainerData.class && (
                                    f.getName().equalsIgnoreCase("metrics")
                            );
                }
                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            })
            .create();
        // Modify the JSON response here based on query params.
        gsonStr = buildResponseBasedOnQuery(mKruizeExperimentMap,gsonObj, latest, results, recommendations, experimentName);
        if (gsonStr.isEmpty()) {
            JSONArray experimentTrialJSONArray = new JSONArray();
            for (String deploymentName : experimentsMap.keySet()) {
                KruizeExperiment kruizeExperiment = experimentsMap.get(deploymentName);
                for (int trialNum : kruizeExperiment.getExperimentTrials().keySet()) {
                    ExperimentTrial experimentTrial = kruizeExperiment.getExperimentTrials().get(trialNum);
                    JSONArray experimentTrialJSON = new JSONArray(TrialHelpers.experimentTrialToJSON(experimentTrial));
                    experimentTrialJSONArray.put(experimentTrialJSON.get(0));
                }
            }
            gsonStr = experimentTrialJSONArray.toString(4);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    private void checkPercentileInfo(Map<String, KruizeObject> mainKruizeExperimentMap) {
        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
        try {
            mainExperimentLoop : for (Map.Entry<String, KruizeObject> entry : mainKruizeExperimentMap.entrySet()) {
                List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                for (K8sObject k8sObject : k8sObjectList) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        Collection<IntervalResults> intervalResultsList;
                        try {
                            intervalResultsList = containerData.getResults().values();
                        } catch (NullPointerException npe) {
                            LOGGER.debug("Result data unavailable. Skipping experiment..");
                            break mainExperimentLoop;
                        }
                        for (IntervalResults intervalResult : intervalResultsList) {
                            for (Map.Entry<AnalyzerConstants.MetricName, MetricResults> innerEntry : intervalResult.getMetricResultsMap().entrySet()) {
                                MetricResults metricResult = innerEntry.getValue();
                                if (!metricResult.isPercentileResultsAvailable()) {
                                    metricResult.setMetricPercentileResults(null);
                                }
                            }
                        }
                        containerDataMap.put(containerData.getContainer_name(), containerData);
                    }
                    k8sObject.setContainerDataMap(containerDataMap);
                }
                entry.getValue().setKubernetes_objects(k8sObjectList);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while checking the percentile: {}", e.getMessage());
        }
    }

    private String buildResponseBasedOnQuery(Map<String, KruizeObject> mKruizeExperimentMap, Gson gsonObj, String latest, String results, String recommendations, String experimentName) throws JsonProcessingException {
        // Fetch experiments data from the DB
        try {
            if (experimentName == null || experimentName.isEmpty())
                new ExperimentDBService().loadAllExperiments(mKruizeExperimentMap);
            else
                new ExperimentDBService().loadExperimentFromDBByName(mKruizeExperimentMap, experimentName);

        } catch (Exception e) {
            LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
        }
        // Case : default
        // return the response without results or recommendations
        String gsonStr = gsonObj.toJson(mKruizeExperimentMap);
        if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE) && recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            gsonStr = modifyJSONResponse(gsonStr, KruizeConstants.JSONKeys.RESULTS);
            gsonStr = modifyJSONResponse(gsonStr, KruizeConstants.JSONKeys.RECOMMENDATIONS);
            return gsonStr;
        } else {
            if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                // fetch results from the DB
                try {
                    if (experimentName == null || experimentName.isEmpty())
                        new ExperimentDBService().loadAllResults(mKruizeExperimentMap);
                    else
                        new ExperimentDBService().loadResultsFromDBByName(mKruizeExperimentMap, experimentName);
                } catch (Exception e) {
                    LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                    return "";
                }
                if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
                    // Case: results=true , recommendations=false
                    // filter the latest results when latest = true, else return all
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        getLatestResults(mKruizeExperimentMap);
                    }
                    checkPercentileInfo(mKruizeExperimentMap);
                    gsonStr = gsonObj.toJson(mKruizeExperimentMap);
                    gsonStr = modifyJSONResponse(gsonStr, KruizeConstants.JSONKeys.RECOMMENDATIONS);
                } else {
                    // fetch recommendations from the DB
                    try {
                        if (experimentName == null || experimentName.isEmpty())
                            new ExperimentDBService().loadAllRecommendations(mKruizeExperimentMap);
                        else
                            new ExperimentDBService().loadRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                        return "";
                    }
                    // Case: results=true, recommendations=true, latest=true
                    // get only latest results and recommendations
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        getLatestResults(mKruizeExperimentMap);
                        getLatestRecommendations(mKruizeExperimentMap);
                    }
                    checkPercentileInfo(mKruizeExperimentMap);
                    gsonStr = gsonObj.toJson(mKruizeExperimentMap);
                    return gsonStr;
                }
            } else {
                if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                    // fetch recommendations from the DB
                    try {
                        if (experimentName == null || experimentName.isEmpty())
                            new ExperimentDBService().loadAllRecommendations(mKruizeExperimentMap);
                        else
                            new ExperimentDBService().loadRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                        return "";
                    }
                    // Case: result=false, recommendations=true, latest=true
                    // filter the latest recommendations when latest = true, else return all
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        getLatestRecommendations(mKruizeExperimentMap);
                        gsonStr = gsonObj.toJson(mKruizeExperimentMap);
                    }
                    gsonStr = modifyJSONResponse(gsonStr, KruizeConstants.JSONKeys.RESULTS);
                }
            }
        }
        return gsonStr;
    }

    private String modifyJSONResponse(String gsonStr, String objectTobeRemoved) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(gsonStr);
        // Find the corresponding object and remove it
        rootNode.findParents(objectTobeRemoved).forEach(parent -> {
            if (parent instanceof ObjectNode)
                ((ObjectNode) parent).remove(objectTobeRemoved);
        });
        // Convert the modified JsonNode back to a JSON string and return it
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }
    private void getLatestResults(Map<String, KruizeObject> mKruizeExperimentMap) {
        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
        try {
            mainExperimentLoop: for (Map.Entry<String, KruizeObject> entry : mKruizeExperimentMap.entrySet()) {
                List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                for (K8sObject k8sObject : k8sObjectList) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        if (null == containerData.getResults()) {
                            LOGGER.warn("Result data is missing for the experiment: {}", entry.getValue().getExperimentName());
                            break mainExperimentLoop;
                        }
                        containerData = Converters.KruizeObjectConverters.getLatestResults(containerData);
                        containerDataMap.put(containerData.getContainer_name(), containerData);
                    }
                    k8sObject.setContainerDataMap(containerDataMap);
                }
                entry.getValue().setKubernetes_objects(k8sObjectList);
            }
        } catch (NullPointerException npe) {
            LOGGER.error("Exception occurred while fetching results for the : {}", npe.getMessage());
        }
    }
    private void getLatestRecommendations(Map<String, KruizeObject> mKruizeExperimentMap) {
        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
        for (Map.Entry<String, KruizeObject> entry : mKruizeExperimentMap.entrySet()) {
            List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
            for (K8sObject k8sObject : k8sObjectList) {
                for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                    containerData = Converters.KruizeObjectConverters.getLatestRecommendations(containerData);
                    containerDataMap.put(containerData.getContainer_name(), containerData);
                }
                k8sObject.setContainerDataMap(containerDataMap);
            }
            entry.getValue().setKubernetes_objects(k8sObjectList);
        }
    }
}
