/*
 * Odilon Object Storage
 * (c) kbee 
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
package io.odilon.test.regression;

//import javax.annotation.PostConstruct;

import io.odilon.client.ODClient;
import io.odilon.client.OdilonClient;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.util.Check;

public abstract class Executor implements Runnable {
	
	static public Logger logger = Logger.getLogger(Executor.class.getName());
	
	static final long DEFAULT_SLEEP_TIME = 1 * 60 * 1000; // 1 minute
	
	private long sleepTime = DEFAULT_SLEEP_TIME;
	
	private boolean exit = false;
	
	private OdilonClient client;
	private  Bucket testBucket;
	boolean isError = false;

	
	String errorStr;
	Exception exception;
	
	
	public Executor() {
	}

	public Executor(long sleepTime, OdilonClient client, Bucket bucket) {
		this.sleepTime=sleepTime;
		this.client=client;
		this.testBucket=bucket;
	}
	
	//@PostConstruct
	public abstract void init();
	
	
	public String ping() {
        return getClient().ping();
	}
	
	
	public abstract void executeTask();
	

	public Exception getException() {
		return exception;
	}
	
	
						
	public void error(String errorStr) {
		this.errorStr=errorStr;
		isError=true;
	}
	
	public void error(Exception e) {
		this.errorStr=e.toString();
		exception=e;
		isError=true;
	}
	
	public void setClient(ODClient client) {
		this.client = client;
	}

	
	public boolean exit() {
		return exit;
	}
	
	public void sendExitSignal() {
		exit=true;
		//this.notify();
	}
	
	public long getSleepTimeMillis() { 
		return sleepTime;
	}

	public OdilonClient getClient() {
		return client;
	}
	 
	public boolean isError() {
		return isError;
	}
	
	
	public void run() {
		
		init();
		
		Check.checkTrue(getSleepTimeMillis()>100, "getSleepTimeMillis() must be > 100 milisecs -> " + String.valueOf(getSleepTimeMillis()));
		
		
		while (!exit()) {
				try {
					Thread.sleep(getSleepTimeMillis());
					executeTask();
				} catch (InterruptedException e) {
				
				} catch (Exception e) {
					logger.error(e);
					throw e;
				}
		}
	}

	public Bucket getTestBucket() {
		return testBucket;
	}

	public void setTestBucket(Bucket testBucket) {
		this.testBucket = testBucket;
	}

}
