package com.autotune.experimentManager.data.result;

import com.autotune.experimentManager.utils.EMUtil;

import java.util.Date;

/**
 * Holds workflow level details like Status and timestamp
 * Workflows are pre/post Validation, Deployment etc
 */
public class StepsMetaData {
    private String stepName;
    private Date beginTimestamp;
    private Date endTimestamp;
    private EMUtil.EMExpStatus status;
    private int iterationNumber;

    public Date getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(Date beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public Date getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Date endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public EMUtil.EMExpStatus getStatus() {
        return status;
    }

    public void setStatus(EMUtil.EMExpStatus status) {
        this.status = status;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public void setIterationNumber(int iterationNumber) {
        this.iterationNumber = iterationNumber;
    }
}
