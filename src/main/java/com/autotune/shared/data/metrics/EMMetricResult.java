package com.autotune.shared.data.metrics;

import com.autotune.shared.data.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricResult implements ConvertToJSON {
    private EMMetricGenericResults emMetricGenericResults;
    private EMMetricPercentileResults emMetricPercentileResults;
    private boolean isPercentileResultsAvailable;

    public boolean isPercentileResultsAvailable() {
        return isPercentileResultsAvailable;
    }

    public void setPercentileResultsAvailable(boolean percentileResultsAvailable) {
        isPercentileResultsAvailable = percentileResultsAvailable;
    }

    public EMMetricResult () {
        emMetricGenericResults = new EMMetricGenericResults();
        emMetricPercentileResults = new EMMetricPercentileResults();
    }

    public EMMetricResult(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(AutotuneConstants.JSONKeys.GENERAL_INFO) ||
            !jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            throw new IncompatibleInputJSONException();
        }
        if (jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            isPercentileResultsAvailable = true;
        }
        emMetricGenericResults = new EMMetricGenericResults(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.GENERAL_INFO));
        emMetricPercentileResults = new EMMetricPercentileResults(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.PERCENTILE_INFO));
    }


    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.GENERAL_INFO, emMetricGenericResults.toJSON());
        if (isPercentileResultsAvailable) {
            jsonObject.put(AutotuneConstants.JSONKeys.PERCENTILE_INFO, emMetricPercentileResults.toJSON());
        }
        return jsonObject;
    }
}
