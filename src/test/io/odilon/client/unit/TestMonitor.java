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


import java.util.Map;


import io.odilon.log.Logger;
import io.odilon.model.SystemInfo;
import io.odilon.test.base.BaseTest;

public class TestMonitor extends BaseTest {	

	private static final Logger logger = Logger.getLogger(TestMonitor.class.getName());
	
	
	public TestMonitor() {
		
	}
	
	
	
	@Override
	public void executeTest() {
		preCondition();
		testSystemInfo();
		showResults();
	}
	
	
	/**
	 * 
	 */
	public boolean testSystemInfo() {
		
		try {
			
			
			
			SystemInfo info = getClient().systemInfo();
			Map<String, String> map = info.getColloquial();
			map.forEach((k,v) -> logger.debug(k + " -> " + v));
			getMap().put("testSystemInfo", "ok");

		} catch (Exception e) {
			error(e);
		}
		
		return true;
		
	}

}
