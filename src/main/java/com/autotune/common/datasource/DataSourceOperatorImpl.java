package com.autotune.common.datasource;

import com.autotune.common.datasource.prometheus.PrometheusDataOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.LoggerFactory;

public class DataSourceOperatorImpl implements DataSourceOperator{

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSourceOperatorImpl.class);
    private static DataSourceOperatorImpl dataSourceOperator = null;
    protected DataSourceOperatorImpl() {
    }

    /**
     * Returns the instance of DataSourceOperatorImpl class
     * @return DataSourceOperatorImpl instance
     */
    public static DataSourceOperatorImpl getInstance() {
        if (null == dataSourceOperator) {
            dataSourceOperator = new DataSourceOperatorImpl();
        }
        return dataSourceOperator;
    }

    /**
     * Returns the instance of specific operator class based on provider type
     * @param provider String containg the name of provider
     * @return instance of specific operator
     */
    @Override
    public DataSourceOperatorImpl getOperator(String provider) {
        if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            return PrometheusDataOperatorImpl.getInstance();
        }
        return null;
    }

    /**
     * Returns the default service port for prometheus
     * @return String containing the port number
     */
    @Override
    public String getDefaultServicePortForProvider(){
        return "";
    }

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     * @param dataSourceUrl String containing the url for the datasource
     * @return DatasourceReachabilityStatus
     */
    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl){
        return null;
    }

    /**
     * executes specified query on datasource and returns the result value
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    @Override
    public Object getValueForQuery(String url, String query){
        return null;
    }

}
