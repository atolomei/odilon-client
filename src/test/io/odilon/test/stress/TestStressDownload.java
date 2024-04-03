package io.odilon.test.stress;
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
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.Check;


public class TestStressDownload implements Runnable {
		
	static private Logger logger = Logger.getLogger(TestStressDownload.class.getSimpleName());	
	static final int DOWNLOADERS = 4;
	
	private Thread thread;
	private final Bucket bucket;
	private AtomicBoolean exit = new AtomicBoolean(false);
	private AtomicBoolean isError = new AtomicBoolean(false);	
	private final BaseTest tester;
	private Map<String, TestFile> testFiles;
	private Downloader dn[];

	private final long start_ms = System.currentTimeMillis();
	
	private boolean done=false;
	private OffsetDateTime start;
	
	
	
	
	/**
	 * @param bucket
	 * @param testFiles
	 * @param tester
	 */
	public TestStressDownload(Bucket bucket, Map<String, TestFile> testFiles, BaseTest tester) {
		
		Check.requireNonNullArgument(bucket, "bucket can not be null");
		Check.requireNonNullArgument(testFiles, "testFiles can not be null");
		Check.requireNonNullArgument(tester, "BaseTest can not be null");
		this.tester=tester;
		this.testFiles=testFiles;
		this.bucket=bucket;
		
		
	}
	
	public void start() {
		this.thread = new Thread(this);
		this.thread.setDaemon(true);
		this.thread.setName(TestStressDownload.this.getClass().getSimpleName());
		this.thread.setPriority(1);
		this.thread.start();
	}
	
	@Override
	public void run() {
		
		dn  = new Downloader[ DOWNLOADERS ];
		this.start = OffsetDateTime.now();
		
		for (int n=0; n<DOWNLOADERS; n++) {
				//dn[n]=new Downloader(Downloader.class.getSimpleName()+"_" + String.valueOf(n));
				//dn[n].start();
		}

		while (!done) {
			try {
				Thread.sleep(2000);
				if (exit.get() || (OffsetDateTime.now().isAfter(start.plusMinutes(1)))) 
						done=true;
				
				logger.debug("aca");
				
			} catch (InterruptedException e) {
			}
		}
		
		for (int n=0; n<DOWNLOADERS; n++) {
			dn[n].sendExitSignal();
		}
	}

	public boolean exit() {
		return exit.get();
	}
	
	public void sendExitSignal() {
		exit.set(true);
		//this.notify();
	}
	
	public AtomicBoolean isError() {
		return isError;
	}

	
	
}
