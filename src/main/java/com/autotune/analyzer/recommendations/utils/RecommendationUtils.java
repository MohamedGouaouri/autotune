package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class RecommendationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationUtils.class);

    public static RecommendationConfigItem getCurrentValue(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                           Timestamp timestampToExtract,
                                                           AnalyzerConstants.ResourceSetting resourceSetting,
                                                           AnalyzerConstants.RecommendationItem recommendationItem,
                                                           ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        Double currentValue = null;
        String format = null;
        RecommendationConfigItem recommendationConfigItem = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu)
                    metricName = AnalyzerConstants.MetricName.cpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.memory)
                    metricName = AnalyzerConstants.MetricName.memoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu)
                    metricName = AnalyzerConstants.MetricName.cpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.memory)
                    metricName = AnalyzerConstants.MetricName.memoryLimit;
            }
            if (null != metricName) {
                if (intervalResults.getMetricResultsMap().containsKey(metricName)) {
                    Optional<MetricResults> metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentValue = metricResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(null);
                    format = metricResults.map(m -> m.getAggregationInfoResult().getFormat()).orElse(null);
                }
                if (null == currentValue) {
                    setNotificationsFor(resourceSetting, recommendationItem, notifications);
                }
                return new RecommendationConfigItem(currentValue, format);
            }
        }
        setNotificationsFor(resourceSetting, recommendationItem, notifications);
        return null;
    }

    private static void setNotificationsFor(AnalyzerConstants.ResourceSetting resourceSetting,
                                            AnalyzerConstants.RecommendationItem recommendationItem,
                                            ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        // Check notifications is null, If it's null -> return.
        if (null == notifications)
            return;
        // Check if the item is CPU
        if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET
                );
            }
        }
        // Check if the item is Memory
        else if (recommendationItem == AnalyzerConstants.RecommendationItem.memory) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET
                );
            }
        }
    }

    public static double checkIfMinDataAvailableForTerm(ContainerData containerData) {
        double sum = 0.0;
        // Check if data available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return sum;
        }

        // get the sum of the durations
        sum = getDurationSummation(containerData);
        return sum;
    }

    public static double getDurationSummation(ContainerData containerData) {
        // Loop over the data to check if there is min data available
        double sum = 0.0;
        for (IntervalResults intervalResults : containerData.getResults().values()) {
            sum = sum + intervalResults.getDurationInMinutes();
        }
        return sum;
    }

    public static Timestamp getMonitoringStartTime(HashMap<Timestamp, IntervalResults> resultsHashMap,
                                                   Timestamp endTime,
                                                   Double durationInHrs) {

        // Convert the HashMap to a TreeMap to maintain sorted order based on IntervalEndTime
        TreeMap<Timestamp, IntervalResults> sortedResultsHashMap = new TreeMap<>(Collections.reverseOrder());
        sortedResultsHashMap.putAll(resultsHashMap);

        double sum = 0.0;
        Timestamp intervalEndTime = null;
        for (Timestamp timestamp : sortedResultsHashMap.keySet()) {
            if (!timestamp.after(endTime)) {
                if (sortedResultsHashMap.containsKey(timestamp)) {
                    sum = sum + sortedResultsHashMap.get(timestamp).getDurationInMinutes();
                    if (sum >= (KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.SHORT_TERM_MIN_DATA_THRESHOLD_MINS
                            - (KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS / KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE))) {
                        // Storing the timestamp value in startTimestamp variable to return
                        intervalEndTime = timestamp;
                        break;
                    }
                }
            }
        }
        try {
            return sortedResultsHashMap.get(intervalEndTime).getIntervalStartTime();
        } catch (NullPointerException npe) {
            LOGGER.error("Exception occurred while getting MonitoringStartTime: {}", npe.getMessage());
            return null;
        }
    }

    public static RecommendationNotification getNotificationForTermAvailability(RecommendationConstants.RecommendationTerms recommendationTerm) {
        RecommendationNotification recommendationNotification = null;
        if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.SHORT_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.MEDIUM_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.LONG_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE);
        }
        return recommendationNotification;
    }


}

