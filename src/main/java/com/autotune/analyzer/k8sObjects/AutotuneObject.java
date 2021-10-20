/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.DAConstants;

import java.util.HashMap;

/**
 * Container class for the Autotune kubernetes kind objects.
 *
 * Refer to examples dir for a reference AutotuneObject yaml.
 */
public final class AutotuneObject
{
	private final String id;
	private final String name;
	private final String namespace;
	private final String mode;
	private final SloInfo sloInfo;
	private final SelectorInfo selectorInfo;

	public AutotuneObject(String id,
			String name,
			String namespace,
			String mode,
			SloInfo sloInfo,
			SelectorInfo selectorInfo) throws InvalidValueException {
		this.id = id;
		HashMap<String, Object> map = new HashMap<>();
		map.put(DAConstants.AutotuneObjectConstants.NAME, name);
		map.put(DAConstants.AutotuneObjectConstants.NAMESPACE, namespace);
		map.put(DAConstants.AutotuneObjectConstants.MODE, mode);
		map.put(DAConstants.AutotuneObjectConstants.SLO, sloInfo);
		map.put(DAConstants.AutotuneObjectConstants.SELECTOR, selectorInfo);

		StringBuilder error = ValidateAutotuneObject.validate(map);
		if (error.toString().isEmpty()) {
			this.name = name;
			this.namespace = namespace;
			this.mode = mode;
			this.sloInfo = sloInfo;
			this.selectorInfo = selectorInfo;
		} else {
			throw new InvalidValueException(error.toString());
		}
	}

	public String getName() {
		return name;
	}

	public SloInfo getSloInfo() {
		return new SloInfo(sloInfo);
	}

	public SelectorInfo getSelectorInfo() {
		return new SelectorInfo(selectorInfo);
	}

	public String getMode() {
		return mode;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "AutotuneObject{" +
				"name='" + name + '\'' +
				", namespace='" + namespace + '\'' +
				", mode='" + mode + '\'' +
				", sloInfo=" + sloInfo +
				", selectorInfo=" + selectorInfo +
				'}';
	}
}
