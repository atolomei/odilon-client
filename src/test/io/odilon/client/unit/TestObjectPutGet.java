 package io.odilon.client.unit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.ODFileUtils;

/**
 * 
 * Put Object
 * Get Object
 * Get PresignedUrl
 * Remove Object
 *
 */
public class TestObjectPutGet extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	
	static final int BUFFER_SIZE = 8192;
	
	int MAX = 40;
	long MAX_LENGTH =20 * 100 * 10000; // 20 MB
		
	long LAPSE_BETWEEN_PUT_MILLISECONDS = 1600;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private final File saveDir = new File(DOWNLOAD_DIR_V0);
	
	private OffsetDateTime showStatus = OffsetDateTime.now();


	/**
	 * 
	 */
	public TestObjectPutGet() {
		
		String max = System.getProperty("max");
		String maxLength = System.getProperty("maxLength");
		String lapse = System.getProperty("lapseBetweenPutSeconds");
		
		if (max!=null)
			MAX = Integer.valueOf(max.trim());
		
		if (maxLength!=null)
			MAX_LENGTH = Long.valueOf(maxLength.trim());
		
		if (lapse!=null)
			LAPSE_BETWEEN_PUT_MILLISECONDS  = Long.valueOf(lapse.trim()); 
	}
	
	/**
	 * 
	 * 
	 * 
	 */
	@Test	
	public void executeTest() {

		preCondition();

		if (!testAddObjects())
			error("testAddObjects");

		
		if (!testAddObjectsStream("java http"))
			error("testAddObjectsStream java http");
	
		
		showResults();
	}
	
	/**
	 */
	public boolean testAddObjectsStream(String version) {
		
	    Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
        				
	    final File dir = new File(SRC_DIR_V0);
	    
        if ((!dir.exists()) || (!dir.isDirectory()))  
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
		
        if ( (!saveDir.exists()) || (!saveDir.isDirectory())) {
	        try {
				FileUtils.forceMkdir(saveDir);
			} catch (IOException e) {
					logger.error(e);
					error(e.getClass().getName() + " | " + e.getMessage());
			}
        }

        int max=MAX / 2;
        	
		int counter = 0;
		String bucketName = this.bucket_1.getName();
		
		for (File fi:dir.listFiles()) {
				
				if (counter >= max)
					break;
				
				if (isElegible(fi)) {
					
					String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*10000))).intValue());;

					try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fi))) {
						
						getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(fi.getName()), Optional.empty());
						testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
						counter++;
						
						sleep();
						
						if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
							logger.info( "testAddObjectsStream -> " + String.valueOf(testFiles.size()));
							showStatus = OffsetDateTime.now();
						}
						
					} catch (ODClientException e) {
						error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
					} catch (FileNotFoundException e1) {
						error(e1);
					} catch (IOException e2) {
						error(e2);
					}
					
				}
			}
			
			logger.info( "testAddObjectsStream total -> " + String.valueOf(testFiles.size()));
			
			testFiles.forEach((k,v) -> {
					
				ObjectMetadata meta = null;
				try {
					 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
				} catch (ODClientException e) {
						logger.error(e);
						System.exit(1);
				}
					
				String destFileName = DOWNLOAD_DIR_V0 + File.separator + meta.fileName;
				
				try {
					getClient().getObject(meta.bucketName, meta.objectName, destFileName);
				} catch (ODClientException | IOException e) {
						logger.error(e);
						System.exit(1);
				}
				
				TestFile t_file=testFiles.get(meta.bucketName+"-"+meta.objectName);
				
				if (t_file!=null) {
					
					try {
					
						String src_sha = t_file.getSrcFileSha256(0);
						String new_sha = ODFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							StringBuilder str  = new StringBuilder();
							str.append("Error sha256 are not equal -> " + meta.bucketName+" / "+meta.objectName);
							str.append(" | src -> " + String.valueOf(t_file.getSrcFile(0).length()) + "bytes");
							str.append(" | dest -> " + String.valueOf(new File(destFileName).length()) + "bytes");
							error(str.toString());
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
						error(e);
					}
				}
				else {
					error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
			});
			
			getMap().put("testAddObjectsStream " + version + " | " + String.valueOf(testFiles.size()), "ok");
				
			return true;
	}


	/**
	 * 
	 * 
	 * @return
	 */
	public boolean testAddObjects() {
		
        File dir = new File(SRC_DIR_V0);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
		}
        
		int counter = 0;
		
		String bucketName = null;
		bucketName = this.bucket_1.getName();
			
		int max=MAX / 2;
		
		// put files
		//
		for (File fi:dir.listFiles()) {
			
			if (counter >= max)
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
						logger.info( " testAddObjects -> " + String.valueOf(testFiles.size()));
						showStatus = OffsetDateTime.now();
					}


					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		logger.info( " testAddObjects total -> " + String.valueOf(testFiles.size()));
		
		
		// -----------
		
		testFiles.forEach( (k,v) -> {
		
			ObjectMetadata meta = null;
				
				try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = DOWNLOAD_DIR_V6+ File.separator + meta.fileName;
				
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
							throw new RuntimeException("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
						throw new RuntimeException(e);
					}
				}
				else {
						error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
				
		
		});
	
		getMap().put("testAddObjects" + " | " + String.valueOf(testFiles.size()), "ok");
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
	        File dir = new File(SRC_DIR_V6);
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V6);
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
            File tmpdir = new File(DOWNLOAD_DIR_V6);
            
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

        
        
        
        String bucketTest = "dev-test";
        
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
	
	
	/**
	 * 
	 * 
	 * @param file
	 * @return
	 */

	private boolean isElegible(File file) {
		
		if (file.isDirectory())
			return false;
		
		if (file.length()>MAX_LENGTH)
			return false;
		
		if (	FSUtil.isText(file.getName()) || 
				FSUtil.isText(file.getName()) || 
				FSUtil.isPdf(file.getName())  || 
				FSUtil.isImage(file.getName()) || 
				FSUtil.isMSOffice(file.getName()) || 
				FSUtil.isZip(file.getName()))
			
			return true;
		
		return false;
	}


	protected void sleep() {
		
		if (LAPSE_BETWEEN_PUT_MILLISECONDS>0) {
			try {
				Thread.sleep(LAPSE_BETWEEN_PUT_MILLISECONDS);
			} catch (InterruptedException e) {
			}
		}
	}

	
}






