#!/bin/bash
#
# Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

###############################  utilities  #################################

function check_running() {
	
	check_pod=$1
    prometheus_ns="monitoring"
	kubectl_cmd="kubectl -n ${prometheus_ns}"

	echo "Info: Waiting for ${check_pod} to come up..."
	while true;
	do
		sleep 2
		${kubectl_cmd} get pods | grep ${check_pod}
		pod_stat=$(${kubectl_cmd} get pods | grep ${check_pod} | awk '{ print $3 }' | grep -v 'Terminating')
		case "${pod_stat}" in
			"ContainerCreating"|"Terminating"|"Pending")
				sleep 2
				;;
			"Running")
				echo "Info: ${check_pod} deploy succeeded: ${pod_stat}"
				err=0
				break;
				;;
			*)
				echo "Error: ${check_pod} deploy failed: ${pod_stat}"
				err=-1
				break;
				;;
		esac
	done

	${kubectl_cmd} get pods | grep ${check_pod}
	echo
}
