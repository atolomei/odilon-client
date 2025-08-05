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
package io.odilon.test.stress;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.Check;


public class Downloader implements Runnable {
			
	static private Logger logger = Logger.getLogger(Downloader.class.getName());
	
	private AtomicBoolean dn_exit = new AtomicBoolean(false);
	private Thread dn_thread;
	private String dnName;

	
	private Bucket bucket = null;
	private BaseTest tester;
	private Map<String, TestFile> testFiles;

	
	public Downloader(String name, Bucket bucket, Map<String, TestFile> testFiles, BaseTest tester) {

		Check.requireNonNullArgument(name, "name can not be null");
		Check.requireNonNullArgument(bucket, "bucket can not be null");
		Check.requireNonNullArgument(testFiles, "testFiles can not be null");
		Check.requireNonNullArgument(tester, "BaseTest can not be null");

		this.dnName=name;
		this.tester=tester;
		this.testFiles=testFiles;
		this.bucket=bucket;
		
	}
	
	public void start() {
		this.dn_thread = new Thread(this);
		this.dn_thread.setDaemon(true);
		this.dn_thread.setName(dnName);
		this.dn_thread.setPriority(1);
		this.dn_thread.start();			
	}

	@Override
	public void run() {
		logger.debug(dnName);
		synchronized (this) {
			while (!dn_exit.get()) {
				try {
					Thread.sleep(1000);
					if (!dn_exit.get()) {
						testDownloadFile();
					}
				} catch (InterruptedException e) {
				}	
			}
		}
	}
	
	private void testDownloadFile() {

		Iterator<String> it = testFiles.keySet().iterator();
		it.next();
		
		
	}
	
	public void sendExitSignal() {
		dn_exit.set(true);
		this.dn_thread.interrupt();
	}

}
