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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.*;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class GenerateRecommendations extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateRecommendations.class);

    private static String sendHttpGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        String authToken = "sha256~1QYiqQ8ewoTuJv8XEO7Fx--Csn0WsQEeEW8i6a9OL0k";
        connection.setRequestProperty("Authorization", "Bearer " + authToken);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            // Read error response body
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();

            throw new IOException("HTTP GET request failed with status code: " + responseCode + "\nError Response: " + errorResponse.toString());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Generates recommendations
     *
     * @param request  an {@link HttpServletRequest} object that
     *                 contains
     *                 the request the client has made
     *                 of the servlet
     * @param response an {@link HttpServletResponse} object that
     *                 contains the response the servlet sends
     *                 to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            // Get the values from the request parameters
            String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
            String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
            Timestamp interval_end_time, interval_start_time;

            // Check if experiment_name is provided
            if (experiment_name == null || experiment_name.isEmpty()) {
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY);
                return;
            }

            // Check if interval_end_time is provided
            if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY);
                return;
            }
            if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
                interval_end_time = null;
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr)
                );
                return;
            } else {
                interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr);
                interval_start_time = RecommendationUtils.getMonitoringStartTime(
                        interval_end_time,
                        Double.valueOf(String.valueOf(KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS * 24)));
                LOGGER.info("{} - {}", interval_start_time, interval_end_time);
            }
            LOGGER.debug("experiment_name : {} and interval_end_time : {} ", experiment_name, intervalEndTimeStr);

            List<ExperimentResultData> experimentResultDataList = new ArrayList<>();
            ExperimentResultData experimentResultData = null;
            Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
            KruizeObject kruizeObject = null;
            try {
                long interval_end_time_epoc = interval_end_time.getTime() / 1000 - (interval_end_time.getTimezoneOffset() * 60);
                ;
                long interval_start_time_epoc = interval_start_time.getTime() / 1000 - (interval_start_time.getTimezoneOffset() * 60);
                ;

                new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experiment_name);
                if (null != mainKruizeExperimentMAP.get(experiment_name)) {
                    kruizeObject = mainKruizeExperimentMAP.get(experiment_name);
                }
                // Get DataSource and connect
                // Get MetricsProfile name and list of promQL to fetch
                // Find LongTerm duration  keep Start , End and Step(measurement Duration)
                // prepare <List>ExperimentResultData
                // and call new ExperimentInitiator().generateAndAddRecommendations(kruizeObject, experimentResultDataList, interval_start_time, interval_end_time);    // TODO: experimentResultDataList not required
                Map<AnalyzerConstants.MetricName, String> promQls = new HashMap<>();
                promQls.put(AnalyzerConstants.MetricName.cpuUsage,
                        "%s by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='', namespace!='', namespace!~'kube-.*|openshift|openshift-.*',namespace=\"%s\",container=\"%s\" }[%sm]))"
                );
                List<String> aggregationMethods = Arrays.asList("sum", "avg", "max", "min");
                Double measurementDurationMinutesInDouble = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
                List<K8sObject> kubernetes_objects = kruizeObject.getKubernetes_objects();
                for (K8sObject k8sObject : kubernetes_objects) {
                    String namespace = k8sObject.getNamespace();
                    HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
                    for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
                        ContainerData containerData = entry.getValue();
                        String containerName = containerData.getContainer_name();
                        HashMap<Timestamp, IntervalResults> containerDataResults = new HashMap<>();
                        IntervalResults intervalResults = new IntervalResults();
                        for (Map.Entry<AnalyzerConstants.MetricName, String> metricEntry : promQls.entrySet()) {
                            HashMap<AnalyzerConstants.MetricName, MetricResults> resMap = new HashMap<>();
                            MetricResults metricResults = new MetricResults();
                            MetricAggregationInfoResults metricAggregationInfoResults = new MetricAggregationInfoResults();
                            for (String methodName : aggregationMethods) {
                                String promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                System.out.println(promQL);
                                String podMetricsUrl = null;
                                String podMetricsResponse = null;
                                try {
                                    podMetricsUrl = String.format("%s?query=%s&start=%s&end=%s&step=%s",
                                            "https://prometheus.crcp01ue1.devshift.net/api/v1/query_range",
                                            URLEncoder.encode(promQL, "UTF-8"),
                                            interval_start_time_epoc,
                                            interval_end_time_epoc,
                                            measurementDurationMinutesInDouble.intValue() * 60);
                                    System.out.println(podMetricsUrl);
                                    podMetricsResponse = sendHttpGetRequest(podMetricsUrl);
                                    Gson gson = new Gson();
                                    JsonObject jsonObject = gson.fromJson(podMetricsResponse, JsonObject.class);
                                    JsonArray resultArray = jsonObject.getAsJsonObject("data").getAsJsonArray("result");
                                    if (null != resultArray && resultArray.size() > 0) {
                                        resultArray = jsonObject.getAsJsonObject("data").getAsJsonArray("result").get(0).getAsJsonObject().getAsJsonArray("values");
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
                                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                        Timestamp sTime = interval_start_time;
                                        for (JsonElement element : resultArray) {
                                            JsonArray valueArray = element.getAsJsonArray();
                                            long epochTime = valueArray.get(0).getAsLong();
                                            double value = valueArray.get(1).getAsDouble();
                                            String timestamp = sdf.format(new Date(epochTime * 1000));
                                            Date date = sdf.parse(timestamp);
                                            Timestamp eTime = new Timestamp(date.getTime());
                                            if (containerDataResults.containsKey(eTime)) {
                                                intervalResults = containerDataResults.get(eTime);
                                                resMap = intervalResults.getMetricResultsMap();
                                                metricResults = resMap.get(AnalyzerConstants.MetricName.cpuUsage);
                                                if (null == metricResults) metricResults = new MetricResults();
                                                metricAggregationInfoResults = metricResults.getAggregationInfoResult();
                                                if (null == metricAggregationInfoResults)
                                                    metricAggregationInfoResults = new MetricAggregationInfoResults();
                                            }
                                            Method method = MetricAggregationInfoResults.class.getDeclaredMethod("set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1), Double.class);
                                            method.invoke(metricAggregationInfoResults, value);
                                            //metricAggregationInfoResults.setSum(value);
                                            metricResults.setAggregationInfoResult(metricAggregationInfoResults);
                                            metricResults.setName(String.valueOf(metricEntry.getKey()));
                                            metricResults.setFormat("cores");      //todo
                                            resMap.put(AnalyzerConstants.MetricName.cpuUsage, metricResults);
                                            intervalResults.setMetricResultsMap(resMap);
                                            intervalResults.setIntervalStartTime(sTime);  //Todo this will change
                                            intervalResults.setIntervalEndTime(eTime);
                                            containerDataResults.put(eTime, intervalResults);
                                            sTime = eTime;
                                        }
                                    }
                                } catch (IOException | ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        containerData.setResults(containerDataResults);
                    }
                }
                Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonUTCDateAdapter()).create();  //.setPrettyPrinting()
                String jsonString = gson.toJson(kruizeObject);
                System.out.println(jsonString);
            } catch (Exception e) {
                sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }


        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerBUpdateRecommendations) {
                MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();              //TODO: Executing two identical SQL SELECT queries against the database instead of just one is causing a performance issue. set 'showSQL' flag is set to true to debug.
        try {
            LOGGER.debug(ko.getKubernetes_objects().toString());
            ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                    convertKruizeObjectToListRecommendationSO(
                            ko,
                            false,
                            false,
                            interval_end_time);
            recommendationList.add(listRecommendationsAPIObject);
        } catch (Exception e) {
            LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
        }
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getDeclaringClass() == ContainerData.class && (field.getName().equals("results"))
                        || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals("metrics")));
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        String gsonStr = "[]";
        if (recommendationList.size() > 0) {
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .setExclusionStrategies(strategy)
                    .create();
            gsonStr = gsonObj.toJson(recommendationList);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            e.printStackTrace();
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

}
