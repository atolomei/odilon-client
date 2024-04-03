package io.odilon.client.unit;

import java.util.Map;

import io.odilon.client.OdilonClient;

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
