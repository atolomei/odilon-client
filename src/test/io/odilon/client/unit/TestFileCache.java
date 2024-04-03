package io.odilon.client.unit;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.RedundancyLevel;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.ODFileUtils;

public class TestFileCache extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	static final int BUFFER_SIZE = 8192;
		
	long LAPSE_BETWEEN_PUT_MILLISECONDS = 1600;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private final File saveDir = new File(DOWNLOAD_DIR_V0);
	
	private OffsetDateTime showStatus = OffsetDateTime.now();
	private String bucketTest = "testcache";
	
	public TestFileCache() {
		String max = System.getProperty("max");
		String maxLength = System.getProperty("maxLength");
		String lapse = System.getProperty("lapseBetweenPutSeconds");
		
		if (max!=null)
			setMax(Integer.valueOf(max.trim()));
		
		if (maxLength!=null)
			setMaxLength(Long.valueOf(maxLength.trim()));
		
		if (lapse!=null)
			LAPSE_BETWEEN_PUT_MILLISECONDS  = Long.valueOf(lapse.trim());
		
	}
	
	
	@Override
	public void executeTest() {

		preCondition();

		if (!testFileCache())
			error("testFileCache()");

		showResults();
		
	}
	
	/**
	 * @return
	 */
	public boolean testFileCache() {
		
        File dir = new File(SRC_DIR_V0);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
		}
        
		int counter = 0;
		
		String bucketName = null;
		bucketName = this.bucket_1.getName();
			
		
		MetricsValues metrics = null;
		
		try {
			metrics = getClient().metrics();
		} catch (ODClientException e) {
			error(e);
		}
		
		
		long hit0 = metrics.cacheFileHitCounter;
		long miss0 = metrics.cacheFileMissCounter;
		long disk0 = metrics.cacheFileHardDiskUsage;
				
		
		// put files
		//
		for (File fi:dir.listFiles()) {
			
			if (counter >= getMax())
				break;
			
			if (isElegible(fi)) {
				
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				
				try {
					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					counter++; 
					
					sleep();
					
					/** display status every 4 seconds or so */
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.info( " testFileCache -> " + String.valueOf(testFiles.size()));
						showStatus = OffsetDateTime.now();
					}

					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		logger.info( " testAddObjects total -> " + String.valueOf(testFiles.size()));
		
		
		
		try {
			metrics = getClient().metrics();
		} catch (ODClientException e) {
			error(e);
		}
		
		long hit1 = metrics.cacheFileHitCounter;
		long miss1 = metrics.cacheFileMissCounter;
		long disk1 = metrics.cacheFileHardDiskUsage;
		
		
		
		
		// -----------
		
		testFiles.forEach( (k,v) -> {
		
			ObjectMetadata meta = null;
				
				try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = DOWNLOAD_DIR_V0+ File.separator + meta.fileName;
				
				
				if ((new File(destFileName)).exists()) {
					FileUtils.deleteQuietly(new File(destFileName));
				}
				
				try {
						getClient().getObject(meta.bucketName, meta.objectName, destFileName);
						
				} catch (ODClientException | IOException e) {
						error(e);
				}
				
				TestFile t_file = testFiles.get(meta.bucketName+"-"+meta.objectName);
				
				if (t_file!=null) {
					
					try {
						String src_sha = t_file.getSrcFileSha256(0);
						String new_sha = ODFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							error("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						}
						
							
					} catch (NoSuchAlgorithmException | IOException e) {
						error(e);
					}
				}
				else {
						error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
		});
	
	
	// -----------
		
		testFiles.forEach( (k,v) -> {
		
			ObjectMetadata meta = null;
				
				try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = DOWNLOAD_DIR_V0+ File.separator + meta.fileName;
				
				if ((new File(destFileName)).exists()) {
					FileUtils.deleteQuietly(new File(destFileName));
				}
				
				
				
				try {
						getClient().getObject(meta.bucketName, meta.objectName, destFileName);
						
				} catch (ODClientException | IOException e) {
						error(e);
				}
				
				TestFile t_file = testFiles.get(meta.bucketName+"-"+meta.objectName);
				
				if (t_file!=null) {
					
					try {
						String src_sha = t_file.getSrcFileSha256(0);
						String new_sha = ODFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							error("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						}
						
							
					} catch (NoSuchAlgorithmException | IOException e) {
						error(e);
					}
				}
				else {
						error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
		});
	
		
		try {
			metrics = getClient().metrics();
		} catch (ODClientException e) {
			error(e);
		}
		
		long hit2 = metrics.cacheFileHitCounter;
		long miss2 = metrics.cacheFileMissCounter;
		long disk2 = metrics.cacheFileHardDiskUsage;
		
		try {
		
			Assert.assertTrue(hit2>=testFiles.size());
			Assert.assertTrue((disk2-disk1)>=0);
			
		} catch (Exception e) {
			error(e);
		}
		
		logger.debug("Assert ok (hit2 - hit1) -> " + String.valueOf(hit2) + " - " + String.valueOf(hit1) +"  = " + String.valueOf(hit2-hit1));
		
		getMap().put("testFileCache()" + " | " + String.valueOf(testFiles.size()), "ok");
		
		return true;
	
	}	

	/**
	 * 
	 * 
	 */
	public boolean preCondition() {

		{
	        File dir = new File(SRC_DIR_V0);
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
			}
		}
		

		{
	        File dir = new File(SRC_DIR_V1);
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V1);
			}
		}

		
		{
	        File dir = new File(SRC_DIR_V2);
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V2);
			}
		}
		
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
        
        try {
			if (getClient().systemInfo().redundancyLevel!=RedundancyLevel.RAID_6) {
				error("File cache can only be tested for -> " + RedundancyLevel.RAID_6.getName());
				
			}
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}
        
        
        
        {
        File tmpdir = new File(DOWNLOAD_DIR_V0);
        
        if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
        	try {
				FileUtils.forceDelete(tmpdir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(tmpdir);
				
		} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
		}
        }
        
        
        {
            File tmpdir = new File(DOWNLOAD_DIR_V1);
            
            if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
            	try {
    				FileUtils.forceDelete(tmpdir);
    			} catch (IOException e) {
    				error(e.getClass().getName() + " | " + e.getMessage());
    			}
    		}
           	try {
    				FileUtils.forceMkdir(tmpdir);
    				
    		} catch (IOException e) {
    				error(e.getClass().getName() + " | " + e.getMessage());
    		}
        }
        

        
        {
            File tmpdir = new File(DOWNLOAD_DIR_V2);
            
            if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
            	try {
    				FileUtils.forceDelete(tmpdir);
    			} catch (IOException e) {
    				error(e.getClass().getName() + " | " + e.getMessage());
    			}
    		}
           	try {
    				FileUtils.forceMkdir(tmpdir);
    				
    		} catch (IOException e) {
    				error(e.getClass().getName() + " | " + e.getMessage());
    		}
        }

        
        
        
        
        try {	
			if (!getClient().existsBucket(bucketTest)) {
				getClient().createBucket(bucketTest);
			}
			
			this.bucket_1 = getClient().getBucket(bucketTest);
			
			return true;
			
			
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
			return false;
		}
	}

    	


}
