package io.odilon.test.base;


import org.junit.Test;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;

import java.io.File;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/***
 * 
 */
public abstract class BaseTest {

	private static final Logger logger = Logger.getLogger(BaseTest.class.getName());

	
	private String SRC_DIR = "d:"+File.separator+"test-files";
	private String DOWNLOAD_DIR = "d:"+File.separator+"test-files-download";
	
	
	public String SRC_DIR_V0 = SRC_DIR +   File.separator + "v0";
	public String SRC_DIR_V1 = SRC_DIR + File.separator+"v1";
	
	public String DOWNLOAD_DIR_V0 = DOWNLOAD_DIR+File.separator+"v0";
	public String DOWNLOAD_DIR_V1 = DOWNLOAD_DIR+File.separator+"v1";
	
	public String endpoint = "http://localhost";
	public int port = 9234;

	
	
	private String accessKey = "odilon";
	private String secretKey = "odilon";
	private ODClient client;
	private Bucket testBucket;
	
	static public long THREE_SECONDS = 3000;
	
	private Map<String, String> map = new TreeMap<String, String>();
	
	public BaseTest() {
		logger.debug("Start " + this.getClass().getName());
		
		String tempDir = System.getProperty("tempDir");
		String downloadDir = System.getProperty("downloadDir");
				
		String tempEndpoint = System.getProperty("endpoint");
		String tempPort = System.getProperty("port");
		
		
		if (tempDir!=null)
			SRC_DIR=tempDir.trim();
		
		if (downloadDir!=null)
			DOWNLOAD_DIR=downloadDir.trim();

		if (tempEndpoint!=null)
			endpoint=tempEndpoint.trim();

		if (tempPort!=null)
			port= Integer.valueOf(tempPort.trim());

		
		
		
		try {
			this.client = new ODClient(endpoint, port, accessKey, secretKey);
	        logger.debug(client.toString());
	        
		} catch (Exception e) {
			error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
		}
	}
	
		
	public void removeTestBucket() {
		try {
			
			if (testBucket==null)
				return;
			
			String bucketName = testBucket.getName();
			
			if (getClient().existsBucket(bucketName)) {
					getClient().deleteBucket(bucketName);
					logger.debug("removeTestBucket. " + bucketName +" -> ok");
					getMap().put("removeTestBucket. " + bucketName, "ok");
			}
		} catch (ODClientException e) {
				logger.error(e);
				error(e);
		}
		
	}
	public Bucket createTestBucket(String bucketName) {

		try {
			if (!getClient().existsBucket(bucketName)) {
					getClient().createBucket(bucketName);
					logger.debug("createTestBucket. " + bucketName + " -> ok");
					getMap().put("createTestBucket. " + bucketName, "ok");
			}
			return getClient().getBucket(bucketName);
			
		} catch (ODClientException e) {
			error(e);
			return null;
		}
		
		
			
	}
	
	public boolean testPing() {
		try {
			String p=ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				logger.debug("ping " +  endpoint + " :" + String.valueOf(port) + " -> ok");
				map.put("ping  " +  endpoint + " :" + String.valueOf(port) , "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		return true;
	}
	
	public boolean preCondition() {
		return testPing();
	}
	
	
	public void error(Exception e) {
		error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
	}
	
	public void error(String string) {
		logger.error(string);
		System.exit(1);
	}
	
	public Map<String, String> getMap() {
		return map;
	}
	
	public String ping() {
        return getClient().ping();
	}
	
	@Test
	public abstract void executeTest();
	
	public boolean isPdf(String filename) {
		return filename.toLowerCase().matches("^.*\\.(pdf)$"); 
	}
	
	public void showResults() {
		logger.debug("Results");
		logger.debug("-------");
		getMap().forEach((k,v) -> logger.debug(k+" -> "+ v));
	    logger.debug("done");
		
	}

	public ODClient getClient() { 
		return client;
	}
	
	public String randomString(int size) {
		int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength =  size;
	    Random random = new Random();
	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();
	    return generatedString;
	}

	protected long dateTimeDifference(Temporal d1, Temporal d2, ChronoUnit unit) {
        return unit.between(d1, d2);
    }
}
 