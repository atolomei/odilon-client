/*
 * Odilon Object Storage
 * (C) Novamens 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.odilon.client.unit;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.test.base.BaseTest;

public class TestServiceRequest extends BaseTest {

	@Override
	public void executeTest() {

		preCondition();

		int counter = 0;
		while (counter++ < 100) {
			try {
				((ODClient) getClient()).addServiceRequest("Test");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			} catch (ODClientException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}

	}

	public boolean preCondition() {

		try {
			String p = ping();
			if (p == null || !p.equals("ok"))
				error("ping  -> " + p != null ? p : "null");
			else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		return true;

	}
}
