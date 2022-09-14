package com.autotune.experimentManager.utils;

import java.util.Locale;
import java.util.SplittableRandom;

public class EMConstants {

	private EMConstants() { }
	public static final class DeploymentConstants {
		private DeploymentConstants() { }
		public static final String NAMESPACE = "default";
	}

	public static final class EMEnv {
		public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
		public static final String EM_ONLY_MODE = "EM_ONLY";
	}

	public static final class TransitionClasses {
		private TransitionClasses() { }
		public static final String CREATE_CONFIG = "com.autotune.experimentManager.transitions.TransitionToCreateConfig";
		public static final String DEPLOY_CONFIG = "com.autotune.experimentManager.transitions.TransitionToDeployConfig";
		public static final String INITIATE_TRIAL_RUN_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateTrialRunPhase";
		public static final String INITIAL_LOAD_CHECK = "com.autotune.experimentManager.transitions.TransitionToInitialLoadCheck";
		public static final String LOAD_CONSISTENCY_CHECK = "com.autotune.experimentManager.transitions.TransitionToLoadConsistencyCheck";
		public static final String INITIATE_METRICS_COLLECTION_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateMetricsCollectionPhase";
		public static final String COLLECT_METRICS = "com.autotune.experimentManager.transitions.TransitionToCollectMetrics";
		public static final String CREATE_RESULT_DATA = "com.autotune.experimentManager.transitions.TransitionToCreateResultData";
		public static final String SEND_RESULT_DATA = "com.autotune.experimentManager.transitions.TransitionToSendResultData";
		public static final String CLEAN_OR_ROLLBACK_DEPLOYMENT = "com.autotune.experimentManager.transitions.TransitionToCleanDeployment";
	}

	public static final class DeploymentStrategies {
		private DeploymentStrategies() { }
		public static final String ROLLING_UPDATE = "rollingUpdate";
		public static final String NEW_DEPLOYMENT = "newDeployment";
	}

	public static final class Logs {
		private Logs() { }
		public static final class LoggerSettings {
			private LoggerSettings() { }
			public static final String DEFAULT_LOG_LEVEL = "ALL";
		}
		public static final class ExperimentManager {
			private ExperimentManager() { }
			public static final String INITIALIZE_EM = "Initializing EM";
			public static final String ADD_EM_SERVLETS = "Adding EM Servlets";
		}
		public static final class RunExperiment {
			private RunExperiment() { }
			public static final String START_TRANSITION_FOR_RUNID = "Starting transition {} for RUN ID - {}";
			public static final String END_TRANSITION_FOR_RUNID = "Ending transition {} for RUN ID - {}";
			public static final String RUNNING_TRANSITION_ON_THREAD_ID = "Running Transition on Thread ID - {}";
		}
		public static final class EMExecutorService {
			private EMExecutorService() { }
			public static final String CREATE_REGULAR_EXECUTOR = "Creating regular executor";
			public static final String CREATE_SCHEDULED_EXECUTOR = "Creating scheduled executor";
			public static final String START_EXECUTE_TRIAL = "Starting to execute a trial";
			public static final String START_SCHEDULED_EXECUTE_TRIAL = "Starting to execute a scheduled trial";
			public static final String START_STAGE_PROCESSORS = "Starting stage processors";
		}
	}

	public static final class EMJSONKeys {
		private EMJSONKeys() { }
        public static final String CONTAINERS = "containers";
		public static final String IMAGE_NAME = "image_name";
		public static final String CONTAINER_NAME = "container_name";
        public static final String ITERATIONS = "iterations";

        // Info section
		public static final String INFO = "info";
		public static final String TRIAL_INFO = "trial_info";
		public static final String DATASOURCE_INFO = "datasource_info";
		public static final String TRIAL_ID = "trial_id";
		public static final String TRIAL_NUM = "trial_num";
		public static final String TRIAL_RESULT_URL = "trial_result_url";
		public static final String URL = "url";
		// Settings section
		public static final String TRACKERS = "trackers";
		public static final String SETTINGS = "settings";
		public static final String DEPLOYMENT_TRACKING = "deployment_tracking";
		public static final String DEPLOYMENT_POLICY = "deployment_policy";
		public static final String DEPLOYMENT_SETTINGS = "deployment_settings";
		public static final String TYPE = "type";
		public static final String DEPLOYMENT_INFO = "deployment_info";
		public static final String DEPLOYMENT_NAME = "deployment_name";
		public static final String TARGET_ENV = "target_env";
		public static final String TRIAL_SETTINGS = "trial_settings";
		public static final String TOTAL_DURATION = "total_duration";
		public static final String WARMUP_CYCLES = "warmup_cycles";
		public static final String WARMUP_DURATION = "warmup_duration";
		public static final String MEASUREMENT_CYCLES = "measurement_cycles";
		public static final String MEASUREMENT_DURATION = "measurement_duration";
		// Metadata Section
		public static final String EXPERIMENT_ID = "experiment_id";
		public static final String EXPERIMENT_NAME = "experiment_name";

		// Deployments Section
		public static final String DEPLOYMENTS = "deployments";
		public static final String NAMESPACE = "namespace";
		public static final String METRICS = "metrics";
		public static final String POD_METRICS = "pod_metrics";
		public static final String CONTAINER_METRICS = "container_metrics";
		public static final String CONFIG = "config";
		public static final String NAME = "name";
		public static final String QUERY = "query";
		public static final String DATASOURCE = "datasource";
		public static final String METRIC_INFO = "metric_info";
		public static final String METRICS_RESULTS = "metrics_results";
		public static final String WARMUP_RESULTS = "warmup_results";
		public static final String MEASUREMENT_RESULTS = "measurement_results";
		public static final String ITERATION_RESULT = "iteration_result";
		public static final String GENERAL_INFO = "general_info";
		public static final String RESULTS = "results";
		public static final String SCORE = "score";
		public static final String ERROR = "error";
		public static final String MEAN = "mean";
		public static final String MODE = "mode";
		public static final String SPIKE = "spike";
		public static final String P_50_0 = "50p";
		public static final String P_95_0 = "95p";
		public static final String P_97_0 = "97p";
		public static final String P_99_0 = "99p";
		public static final String P_99_9 = "99.9p";
		public static final String P_99_99 = "99.99p";
		public static final String P_99_999 = "99.999p";
		public static final String P_99_9999 = "99.9999p";
		public static final String P_100_0 = "100p";
		public static final String CYCLES = "cycles";
		public static final String DURATION = "duration";
		public static final String PERCENTILE_INFO = "percentile_info";
	}

	public static final class InputJsonKeys {
		private InputJsonKeys() { }
		public static final class ListTrialStatusKeys {
			private ListTrialStatusKeys() { }
			public static final String RUN_ID ="runId";
			public static final String STATUS = "status";
			public static final String ERROR = "error";
			public static final String SUMMARY = "summary";
			public static final String COMPLETE_STATUS = "completeStatus";
			public static final String EXPERIMENT_NAME = "experiment_name";
			public static final String TRIAL_NUM = "trial_num";
			public static final String VERBOSE = "verbose";
		}
		public static final class DeploymentKeys {
			private DeploymentKeys() { }
			public static final String PARENT_DEPLOYMENT_NAME = "parent_deployment_name";
			public static final String TRAINING_DEPLOYMENT_NAME = "training_deployment_name";
		}
	}

	public static final class EMConfigDeployments {
		private EMConfigDeployments() { }
		public static final class DeploymentTypes {
			private DeploymentTypes() { }
			public static final String TRAINING = "training";
			public static final String PRODUCTION = "production";
		}
	}

	public static final class EMConfigSettings {
		private EMConfigSettings() { }
		public static final class TrialSettings {
			private TrialSettings() { }
		}
	}

	public static final class EMSettings {
		private EMSettings() { }
		// Number of current executors per CPU core
		public static final int EXECUTORS_MULTIPLIER = 1;
		// Maximum number of executors per CPU core
		public static final int MAX_EXECUTORS_MULTIPLIER = 4;
	}

	public static final class EMExecutorService {
		private EMExecutorService() { }
		public static final class EMConfigSettings {
			private EMConfigSettings() { }
			public static final int MIN_EXECUTOR_POOL_SIZE = 1;
		}
	}

	public static final class TimeUnitsExt {
		private TimeUnitsExt() { }

		public static final String SECOND_LC_SINGULAR = "second";
		public static final String SECOND_LC_PLURAL = SECOND_LC_SINGULAR + "s";
		public static final String SECOND_UC_SINGULAR = SECOND_LC_SINGULAR.toUpperCase();
		public static final String SECOND_UC_PLURAL = SECOND_LC_PLURAL.toUpperCase();

		public static final String SECOND_SHORT_LC_SINGULAR = "sec";
		public static final String SECOND_SHORT_LC_PLURAL = SECOND_SHORT_LC_SINGULAR + "s";
		public static final String SECOND_SHORT_UC_SINGULAR = SECOND_SHORT_LC_SINGULAR.toUpperCase();
		public static final String SECOND_SHORT_UC_PLURAL = SECOND_SHORT_LC_PLURAL.toUpperCase();

		public static final String SECOND_SINGLE_LC = "s";
		public static final String SECOND_SINGLE_UC= SECOND_SINGLE_LC.toUpperCase();

		public static final String MINUTE_LC_SINGULAR = "minute";
		public static final String MINUTE_LC_PLURAL = MINUTE_LC_SINGULAR + "s";
		public static final String MINUTE_UC_SINGULAR = MINUTE_LC_SINGULAR.toUpperCase();
		public static final String MINUTE_UC_PLURAL = MINUTE_LC_PLURAL.toUpperCase();

		public static final String MINUTE_SHORT_LC_SINGULAR = "min";
		public static final String MINUTE_SHORT_LC_PLURAL = MINUTE_SHORT_LC_SINGULAR + "s";
		public static final String MINUTE_SHORT_UC_SINGULAR = MINUTE_SHORT_LC_SINGULAR.toUpperCase();
		public static final String MINUTE_SHORT_UC_PLURAL = MINUTE_SHORT_LC_PLURAL.toUpperCase();

		public static final String MINUTE_SINGLE_LC = "m";
		public static final String MINUTE_SINGLE_UC= MINUTE_SINGLE_LC.toUpperCase();

		public static final String HOUR_LC_SINGULAR = "hour";
		public static final String HOUR_LC_PLURAL = HOUR_LC_SINGULAR + "s";
		public static final String HOUR_UC_SINGULAR = HOUR_LC_SINGULAR.toUpperCase();
		public static final String HOUR_UC_PLURAL = HOUR_LC_PLURAL.toUpperCase();

		public static final String HOUR_SHORT_LC_SINGULAR = "hr";
		public static final String HOUR_SHORT_LC_PLURAL = HOUR_SHORT_LC_SINGULAR + "s";
		public static final String HOUR_SHORT_UC_SINGULAR = HOUR_SHORT_LC_SINGULAR.toUpperCase();
		public static final String HOUR_SHORT_UC_PLURAL = HOUR_SHORT_LC_PLURAL.toUpperCase();

		public static final String HOUR_SINGLE_LC = "h";
		public static final String HOUR_SINGLE_UC= HOUR_SINGLE_LC.toUpperCase();
	}

	public static final class TimeConv {
		private TimeConv() { }
		public static final int NO_OF_SECONDS_PER_MINUTE = 60;
		public static final int NO_OF_MINUTES_PER_HOUR = 60;
		public static final int NO_OF_HOURS_PER_DAY = 12;
		public static final int DEPLOYMENT_IS_READY_WITHIN_MINUTE = 2;
		public static final int DEPLOYMENT_CHECK_INTERVAL_IF_READY_MILLIS = 1000;
	}

	public static final class EMJSONValueDefaults {
		private EMJSONValueDefaults() { }
		public static final String TRIAL_ID_DEFAULT = "";
		public static final int TRIAL_NUM_DEFAULT = -1;
		public static final String TRIAL_RESULT_URL_DEFAULT = "";
		public static final String DEFAULT_NULL = null;
		public static final String DEPLOYMENT_TYPE_DEFAULT = "rollingUpdate";
	}

	public static final class StandardDefaults {
		private StandardDefaults() { }
		public static final int NEGATIVE_INT_DEFAULT = -1;
		public static final String CPU_QUERY_NAME = "cpuRequest";
		public static final String MEM_QUERY_NAME = "memRequest";
		public static final String THROUGHPUT = "throughput";
		public static final String RESPONSE_TIME = "response_time";

		public static final class BackOffThresholds {
			private BackOffThresholds() { }
			public static final int CHECK_LOAD_AVAILABILITY_THRESHOLD = 10;
			public static final int DEPLOYMENT_READINESS_THRESHOLD = 10;
			public static final int[] EXPONENTIAL_BACKOFF_INTERVALS = {1, 3, 4, 7, 11};
			public static final int DEFAULT_LINEAR_BACKOFF_INTERVAL = 1;
		}
	}
}
