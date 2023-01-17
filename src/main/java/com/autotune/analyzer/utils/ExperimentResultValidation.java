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
package com.autotune.analyzer.utils;

import com.autotune.common.k8sObjects.ExperimentResultData;
import com.autotune.common.k8sObjects.KruizeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Util class to validate input request related to Experiment Results for metrics collection.
 */
public class ExperimentResultValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentValidation.class);
    private boolean success;
    private String errorMessage;
    private Map<String, KruizeObject> mainKruizeExperimentMAP;

    public ExperimentResultValidation(Map<String, KruizeObject> mainKruizeExperimentMAP) {
        this.mainKruizeExperimentMAP = mainKruizeExperimentMAP;
    }

    public void validate(List<ExperimentResultData> experimentResultDataList) {
        try {
            boolean proceed = false;
            String errorMsg = "";
            for (ExperimentResultData resultData : experimentResultDataList) {
                if (null != resultData.getExperiment_name() || null != resultData.getTrial_timestamp()) {
                    if (mainKruizeExperimentMAP.keySet().contains(resultData.getExperiment_name())) {
                        KruizeObject kruizeObject = mainKruizeExperimentMAP.get(resultData.getExperiment_name());
                        boolean isExist = false;
                        if (null != kruizeObject.getExperimentResultDataSet())
                            isExist = kruizeObject.getExperimentResultDataSet().contains(resultData);
                        if (isExist) {
                            proceed = false;
                            errorMsg = errorMsg.concat(String.format("Experiment name : %s already contains result for timestamp : %s", resultData.getExperiment_name(), resultData.getTrial_timestamp()));
                            break;
                        }
                        proceed = true;
                    } else {
                        proceed = false;
                        errorMsg = errorMsg.concat(String.format("Experiment name : %s not found", resultData.getExperiment_name()));
                        break;
                    }
                } else {
                    errorMsg = errorMsg.concat("experiment_name and timestamp are mandatory fields.");
                    proceed = false;
                    break;
                }
            }
            if (proceed)
                setSuccess(true);
            else
                markFailed(errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage("Validation failed due to : " + e.getMessage());
        }
    }

    public void markFailed(String message) {
        setSuccess(false);
        setErrorMessage(message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ExperimentResultValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
