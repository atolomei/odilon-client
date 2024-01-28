package io.odilon.test.regression;

import java.time.OffsetDateTime;

import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.test.base.BaseTest;


public class TestLoad extends BaseTest {
			
	static public Logger logger = Logger.getLogger(TestLoad.class.getName());
	
	
	final long MAX_DURATIONS =  60 * 2; // 2 min.
	
	private Bucket testBucket;
	
	
	final long readingMetadataLapse=1000 * 4; // 4 secs
	
	
	
	/**
	 * <p>
	 * 1 thread reading metadata file 1x2 secs.
	 * 1 thread downloading file 1x5 secs.
	 * 1 thread uploading files 1x5 secs.
	 * 1 thread deleting files 1x10 secs. 
	 * 1 thread listing files 1x20 secs.
	 * </p>
	 */
	@Override
	public void executeTest() {
		
		
		String ping = ping();
		if (ping.equals("ok"))
			throw new RuntimeException("ping error -> " + ping);
		
		

		
		this.testBucket = createTestBucket("test-load");
		
		ExecutorMetadata eMeta = new ExecutorMetadata(readingMetadataLapse, getClient(), testBucket);
		eMeta.init();
		Thread thread = new Thread(eMeta);
 		thread.setDaemon(true);
 		thread.start();

 		
 		
 		
 		OffsetDateTime start = OffsetDateTime.now();
 		
 		while (OffsetDateTime.now().isBefore(start.plusSeconds(MAX_DURATIONS))) {
 			
 			try {
				
 				Thread.sleep(2000);
 				
				if (eMeta.isError()) {
					break;
				}
				
			} catch (InterruptedException e) {
				logger.error(e);
			}
 		}
 		
 		
 		
 		eMeta.exit();
 		
 		if (eMeta.isError()) {
			logger.error(eMeta.getException());
		}
 		
		showResults();
		
	}
	
	
}

 






