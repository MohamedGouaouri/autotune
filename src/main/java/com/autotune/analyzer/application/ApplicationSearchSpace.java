package com.autotune.analyzer.application;

import java.util.HashMap;
import java.util.Map;

public class ApplicationSearchSpace
{
	private final String experimentName;
	private final String experimentId;
	private final String applicationName;
	private final String objectiveFunction;
	private final String hpoAlgoImpl;
	private final String direction;
	private final String valueType;

	Map<String, Tunable> applicationTunablesMap;

	public ApplicationSearchSpace(String experimentName,
								  String experimentId,
								  String applicationName,
								  String objectiveFunction,
								  String hpoAlgoImpl,
								  String direction,
								  String valueType) {
		this.experimentName = experimentName;
		this.experimentId = experimentId;
		this.applicationName = applicationName;
		this.objectiveFunction = objectiveFunction;
		this.hpoAlgoImpl = hpoAlgoImpl;
		this.direction = direction;
		this.valueType = valueType;

		this.applicationTunablesMap = new HashMap<>();
	}

	public String getExperimentName() {	return experimentName;	}

	public String getExperimentId() {
		return experimentId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getObjectiveFunction() {
		return objectiveFunction;
	}

	public String getHpoAlgoImpl() {
		return hpoAlgoImpl;
	}

	public String getDirection() {
		return direction;
	}

	public Map<String, Tunable> getApplicationTunablesMap() {
		return applicationTunablesMap;
	}

	public String getValueType() {
		return valueType;
	}
}