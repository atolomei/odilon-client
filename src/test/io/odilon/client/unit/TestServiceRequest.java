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
				String p=ping();
				if (p==null || !p.equals("ok"))
					error("ping  -> " + p!=null?p:"null");
				else {
					getMap().put("ping", "ok");
				}
			} catch (Exception e)	{
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		 
		 return true;
		 
	}
}
