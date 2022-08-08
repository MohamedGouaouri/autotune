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
package com.autotune.experimentManager.core;

import com.autotune.common.experiments.ContainerConfigData;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class helper used to deploy application after considering Container config details
 */

public class DeploymentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentHandler.class);
    private final String nameSpace;
    private final String deploymentName;
    private final ContainerConfigData containerConfigData;
    private boolean alreadyDeployedJustRestart = false;
    private final KubernetesServices kubernetesServices;

    public DeploymentHandler(String nameSpace, String deploymentName, ContainerConfigData containerConfigData, KubernetesServices kubernetesServices) {
        this.nameSpace = nameSpace;
        this.deploymentName = deploymentName;
        this.containerConfigData = containerConfigData;
        this.kubernetesServices = kubernetesServices;
    }


    public void initiateDeploy() {
        LOGGER.debug("START DEPLOYING");
        try {
            if (!this.alreadyDeployedJustRestart) {
                //Check here if deployment type is rolling-update   .withName(this.deploymentName)
                this.kubernetesServices.startDeploying(this.nameSpace, this.deploymentName, this.containerConfigData);
                this.alreadyDeployedJustRestart = true;
            } else {
                this.kubernetesServices.restartDeployment(this.nameSpace, this.deploymentName);
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        LOGGER.debug("END DEPLOYING");
    }

    public boolean isDeploymentReady() {
        boolean running = false;
        try {
            running = this.kubernetesServices.isDeploymentReady(this.nameSpace, this.deploymentName);
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        return running;
    }


}
