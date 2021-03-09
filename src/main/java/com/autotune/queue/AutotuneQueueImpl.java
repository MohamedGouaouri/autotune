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
package com.autotune.queue;

import java.util.concurrent.BlockingQueue;

/**
 * AututuneQueueImpl is default implementation of the AutotuneQueue contracts
 * @author bipkumar
 *
 */
public abstract class AutotuneQueueImpl implements AutotuneQueue {
	
	protected BlockingQueue<AutotuneDTO> queue = null;
	protected String name = "";
	
	@Override
	public boolean send(AutotuneDTO data) throws InterruptedException {
		if (queue != null) {
			return queue.offer(data);
		}
			
		return false;
	}

	@Override
	public AutotuneDTO get() throws InterruptedException {
		if (queue != null)
			return queue.take();
		
		return null;
	}

	@Override
	public String getName() {
		return name;
	}
}
