package io.odilon.client.unit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
				
public class TestSetupServer extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestSetupServer.class.getName());
	
	private String buckets[] = {"bucket1", "bucket2", "bucket3", "bucket4", "bucket5"};

	static final int MAX = 3;
	static final long MAX_LENGTH = 100 * 1000; // 100 KB
	
	static final String TEMP_DIR = "c:"+File.separator+"temp";
	private Bucket bucket_1 = null;
	
	
	@Override
	public void executeTest() {
		
		logger.debug("This test requires an empty odilon server");
		logger.debug("It creates " +  String.valueOf(buckets.length) + " buckets and stores " + String.valueOf(MAX)+ " files on one of them");
		
		if (!testListBuckets())
			error("testListBuckets");
		
		if (!testMakeBuckets())
			error("testMakeBuckets");
		
		if (!testAddObjects())
			error("testAddObjects");
		
		showResults();
	}


	/**
	 * 
	 * @return
	 */
	
	public boolean testAddObjects() {
        			
        Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
        				
        File dir = new File(TEMP_DIR);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +TEMP_DIR);
		}
        
		int counter = 0;
		
		
		String bucketName = null;
		
		try {
			bucket_1=getClient().listBuckets().get(0);
			bucketName = bucket_1.getName();
			
		} catch (ODClientException e) {
			logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
			System.exit(1);
		}

		
		// put files
		for (File fi:dir.listFiles()) {
			
			if (counter++ == MAX)
				break;
			
			if (!fi.isDirectory() && (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName())) && (fi.length()<MAX_LENGTH)) {
																	
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				
				try {
					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
    }
		
		
	testFiles.forEach( (k,v) -> {
		try {
				ObjectMetadata  meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
				logger.debug(meta.bucketName +" / " + meta.objectName);
				
			} catch (ODClientException e) {
				error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
			}	
	});
	
	
	logger.debug("done -> testAddObjects");
	getMap().put("testAddObjects", "ok");
	return true;
	
	}


		
	/**
	 * 
	 */
	public boolean testMakeBuckets() {
		
			/**------------------
			 * remove buckets if exist
			 */
			try {
				logger.debug("Total buckets -> " + getClient().listBuckets().size());
				for (String s: buckets) {
					if (getClient().existsBucket(s))  {
						if (getClient().isEmpty(s)) { 
							logger.debug("removing bucket -> " + s);
							getClient().deleteBucket(s);
							org.junit.Assert.assertFalse(getClient().existsBucket(s));
						}
					}
				}
			} catch (Exception e) {
				logger.error(e);
				System.exit(1);
			}
	    	
			/**------------------
			 * create buckets
			 */
			try {
				int n = 0;
				for (String s: buckets) {
					if (!getClient().existsBucket(s)) {
						getClient().createBucket(s);
						n++;
						org.junit.Assert.assertTrue(getClient().existsBucket(s));
					}
				}
				logger.debug("created -> " + String.valueOf(n) + " buckets");
			} catch (Exception e) {
						error(e);
			}

			try {
				for (String s: buckets) {
					if (!getClient().existsBucket(s)) {
						logger.error("bucket does not exist and it should -> " + s);
						org.junit.Assert.assertTrue(getClient().existsBucket(s));
						return false;
					}
				}
				logger.debug("done -> testMakeBuckets");
				getMap().put("makeBuckets", "ok");
				return true;
				
			} catch (Exception e) {
				error(e);
			}

			return false;
	}

	
	public boolean testListBuckets() {
        try {
	        List<Bucket> list = getClient().listBuckets();
	        Assert.assertNotNull(list);
	        for (Bucket bucket: list) {
	        	logger.debug(bucket.getName());
	        }
	        getMap().put("testListBuckets", "ok");
	        logger.debug("done -> testListBuckets");
	        return true;
	        
        }  catch (Exception e) {
			error(e);
    	}
        return false;
	}


}
