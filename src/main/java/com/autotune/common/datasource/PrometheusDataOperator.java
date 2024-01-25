/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.datasource;

import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class PrometheusDataOperator implements KruizeDataSourceOperator {
    private static PrometheusDataOperator prometheusDataOperator = null;
    private PrometheusDataOperator() {

    }
    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusDataOperator.class);
    public static PrometheusDataOperator getInstance() {
        if (null == prometheusDataOperator) {
            prometheusDataOperator = new PrometheusDataOperator();
        }
        return prometheusDataOperator;
    }

    private JSONObject getPrometheusJsonObject(String url, String query) {
        GenericRestApiClient apiClient = new GenericRestApiClient(
                CommonUtils.getBaseDataSourceUrl(
                        url,
                        KruizeConstants.SupportedDatasources.PROMETHEUS
                )
        );

        if (null == apiClient) {
            return null;
        }

        try {
            JSONObject jsonObject = apiClient.fetchMetricsJson(
                    KruizeConstants.HttpConstants.MethodType.GET,
                    query);

            if (!jsonObject.has("status") ||
                    !jsonObject.getString("status").equalsIgnoreCase("success") ||
                    !jsonObject.has("data") ||
                    !jsonObject.getJSONObject("data").has("result") ||
                    jsonObject.getJSONObject("data").getJSONArray("result").isEmpty()) {
                return null;
            }

            return jsonObject;
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }
    @Override
    public Object getPrometheusDataValue(String url, String query) {
        JSONObject jsonObject = getPrometheusJsonObject(url, query);

        if (jsonObject != null) {
            JSONArray result = jsonObject.getJSONObject("data").getJSONArray("result");

            for (Object resultObj : result) {
                JSONObject resultJson = (JSONObject) resultObj;
                if (resultJson.has("value") && !resultJson.getJSONArray("value").isEmpty()) {
                    return resultJson.getJSONArray("value").getString(1);
                }
            }
        }

        return null;
    }

    /**
     * Retrieves Prometheus data from the specified URL and query, returning the result as a JsonArray.
     *
     * @param url   The Prometheus API endpoint URL.
     * @param query The Prometheus query string.
     * @return      A JsonArray containing the "result" data, or null if the data is not available or in the expected format.
     *
     * Example output JsonArray -
     * [
     *   {
     *     "metric": {
     *       "__name__": "exampleMetric"
     *     },
     *     "value": [1642612628.987, "1"]
     *   }
     * ]
     */
    public JsonArray getPrometheusDataResultArray(String url, String query) {
        JSONObject jsonObject = getPrometheusJsonObject(url, query);

        if (jsonObject != null) {
            String jsonString = jsonObject.toString();
            JsonObject parsedJsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonObject dataObject = parsedJsonObject.get("data").getAsJsonObject();

            if (dataObject.has("result") && dataObject.get("result").isJsonArray()) {
                JsonArray resultArray = dataObject.getAsJsonArray("result");

                if (resultArray != null) {
                    return resultArray;
                }
            }
        }
        return null;
    }
}
