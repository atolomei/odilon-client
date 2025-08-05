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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SharedConstant;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.Check;


public class TestStressUploader implements Runnable {
			
	static public Logger logger = Logger.getLogger(TestStressUploader.class.getName());
	
	private  int maxUploadingThread  = 7;
	private AtomicLong total_uploaded = new AtomicLong(0);
	private AtomicLong totalUpload_bytes = new AtomicLong(0);
	private AtomicBoolean isError = new AtomicBoolean(false);

	private List<File> up;
	
	private Bucket bucket = null;
	private BaseTest tester;
	private Map<String, TestFile> testFiles;
	
	private AtomicBoolean exit = new AtomicBoolean(false);
	
	private boolean isDone = false;
	
	private Thread thread;
	
	private String name;
	public TestStressUploader(Bucket bucket, List<File> up, Map<String, TestFile> testFiles, BaseTest tester) {

		Check.requireNonNullArgument(bucket, "bucket can not be null");
		Check.requireNonNullArgument(up, "List can not be null");
		Check.requireNonNullArgument(testFiles, "testFiles can not be null");
		Check.requireNonNullArgument(tester, "BaseTest can not be null");
									
		this.tester=tester;
		this.testFiles=testFiles;
		this.up=up;
		this.bucket=bucket;
		this.name = getClass().getSimpleName();
	}
	
	public void start() {
		this.thread = new Thread(this);
		this.thread.setDaemon(true);
		this.thread.setName(name);
		this.thread.setPriority(1);
		this.thread.start();			
	}
	
	public boolean exit() {
		return exit.get();
	}
	
	public void sendExitSignal() {
		exit.set(true);
		if (thread!=null)
			thread.interrupt();
		
	}
	
	public AtomicBoolean isError() {
		return isError;
	}

	
	public int getUploadingThread() {
		return this.maxUploadingThread;
	}

	public AtomicLong getTotalUploaded() {
		return total_uploaded;
	}
	
	@Override
	public void run() {
		
		long start_ms = System.currentTimeMillis();
		
		this.thread = Thread.currentThread();
		 
		ExecutorService executor = null;
		
		
		try {
			
			executor = Executors.newFixedThreadPool(this.maxUploadingThread);
			
			List<Callable<Object>> tasks = new ArrayList<>(up.size());
			
			for (File file: up) {
				
				tasks.add(() -> {
					
					try {
					
						if (exit() || isError().get())
							return null;
						
						String objectName = FSUtil.getBaseName(file.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*10000))).intValue());;
						InputStream inputStream = null;
						
						try {

							inputStream = new BufferedInputStream(new FileInputStream(file));

									try {
										
											@SuppressWarnings("unused")
											ObjectMetadata meta = this.tester.getClient().putObjectStream( 	this.bucket.getName(), 
																											objectName, 
																											inputStream, 
																											Optional.of(file.getName()),
																											Optional.empty());

											
											
											
											TestFile tf=new TestFile(file,  bucket.getName(), objectName);
											
											this.testFiles.put(bucket.getName()+"-"+objectName, tf);
										
											if ((this.total_uploaded.get()+1) % 50 == 0)
												logger.debug(String.valueOf(total_uploaded.get()) + " | " + file.getName());											
											
											this.total_uploaded.incrementAndGet();
											this.totalUpload_bytes.addAndGet(file.length());
											
									} catch (Exception e) {
										this.tester.error(e);
										this.isError.set(true);
										return null;
									}
						
							} catch (FileNotFoundException e) {
								/** the file may no longer exist, 
								 so we don't consider this an error  */
								logger.warn(e);
							}
						
						} catch (Exception e) {
							logger.error(e);
							this.isError.set(true);
							return null;
						}
					return null;
				 });
			}
				
			try {
				
				if (!exit())
					executor.invokeAll(tasks, 12, TimeUnit.HOURS);
				
			} catch (InterruptedException e) {
				logger.error(e);
			}
			
			try {
					executor.shutdown();
					executor.awaitTermination(5, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				
			}
		
		} finally {
			
			logger.info("Threads: " + String.valueOf(this.maxUploadingThread));
			logger.info("Total Files: " + String.valueOf(this.total_uploaded.get()));
			logger.info("Total Size: " + String.format("%9.2f MB", Double.valueOf(totalUpload_bytes.get()).doubleValue() / SharedConstant.d_megabyte).trim());
			logger.info("Duration: " + String.valueOf(Double.valueOf(System.currentTimeMillis() - start_ms) / Double.valueOf(1000)) + " secs");
			logger.info("---------");
			
			isDone = true;
		}
		
	}

	public boolean isDone() {
		return isDone;
	}
	
	

}
