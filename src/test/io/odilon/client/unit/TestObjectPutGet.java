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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import io.odilon.util.OdilonFileUtils;

/**
 * 
 * Put Object
 * Get Object
 * Get PresignedUrl
 * Remove Object
 *
 *
 */
public class TestObjectPutGet extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	private String sourceDir;
	private String downloadDir;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private OffsetDateTime showStatus = OffsetDateTime.now();
	private int sub_index = 0;
	
	
	public TestObjectPutGet() {
	}
	
	
	@Test	
	public void executeTest() {
		
		preCondition();

		if (!testAddObjectsStream("java http"))
			error("testAddObjectsStream java http");
		
		if (!testAddObjects())
			error("testAddObjects");
	
		 showResults();
	}
	
	
	
	/**
	 */
	public boolean testAddObjectsStream(String version) {
		
	    Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
        	
	    downloadDir = super.DOWNLOAD_DIR_V0;
	    sourceDir = super.SRC_DIR_V0;
	    
	    final File dir = new File(sourceDir);
	    final File dndir = new File(downloadDir);
	    
        if ((!dir.exists()) || (!dir.isDirectory()))  
			error("Dir not exists or the File is not Dir -> " + sourceDir);
		
        if ( (!dndir.exists()) || (!dndir.isDirectory())) {
	        try {
				FileUtils.forceMkdir(dndir);
			} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
        }
        
        	
		int counter = 0;
		String bucketName = this.bucket_1.getName();
		
		for (File file : dir.listFiles()) {
				
				if (counter >=  (int) Math.round( Double.valueOf(Double.valueOf(getMax()).doubleValue()/2.0).doubleValue()))
					break;
				
				if (isElegible(file)) {
					
					String objectName = FSUtil.getBaseName(file.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*10000))).intValue());;

					try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
						
						
						List<String> customTags = new ArrayList<String>();
						customTags.add(String.valueOf(counter));
						
						getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.empty(), Optional.empty(), Optional.ofNullable(customTags));
						
						testFiles.put(bucketName+"-"+objectName, new TestFile(file, bucketName, objectName));
						logger.info( String.valueOf(testFiles.size() + " testAddObjectsStream -> " + file.getName()));
						counter++;
						
						sleep();
						
						if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
							logger.info( "testAddObjectsStream add -> " + String.valueOf(testFiles.size()));
							showStatus = OffsetDateTime.now();
						}
						
					} catch (ODClientException e) {
						error("Http status " + String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
					} catch (FileNotFoundException e1) {
						error(e1);
					} catch (IOException e2) {
						error(e2);
					}
					
				}
			}
			
			logger.info( "testAddObjectsStream add total -> " + String.valueOf(testFiles.size()));
		
			
			sub_index = 0;
			
			
			testFiles.forEach((k,v) -> {
					
			    
				ObjectMetadata meta = null;
				
				
				
				try {
					 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
					 sub_index++;
					 
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = downloadDir + File.separator + meta.fileName;
				
				if (new File(destFileName).exists())
					FileUtils.deleteQuietly(new File(destFileName));
				
				try {

					getClient().getObject(meta.bucketName, meta.objectName, destFileName);
					
				} catch (ODClientException | IOException e) {
					error(e);
				}
				

									
					try {
					
						String src_sha = v.getSrcFileSha256(0);
						String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							StringBuilder str  = new StringBuilder();
							str.append("testAddObjectsStream Error sha256 are not equal -> " + meta.bucketName+" / "+meta.objectName);
							str.append(" | src -> " + v.getSrcFile(0).getAbsolutePath()           + "  " + String.valueOf(v.getSrcFile(0).length()/1000.0) + " kbytes");
							str.append(" | dest -> "+ (new File(destFileName)).getAbsolutePath()  + "  " + String.valueOf(new File(destFileName).length()/1000.0) + " kbytes");
							error(str.toString());
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
						logger.error(e);
						error(e);
					}
					
                    if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
                        logger.info( "testAddObjectsStream check -> " + String.valueOf(sub_index));
                        showStatus = OffsetDateTime.now();
                    }
			});
			

			logger.info( "testAddObjectsStream -> ok " + String.valueOf(testFiles.size()));
			
			getMap().put("testAddObjectsStream " + version + " | " + String.valueOf(testFiles.size()), "ok");
				
			return true;
	}


	/**
	 * 
	 * 
	 * @return
	 */
	public boolean testAddObjects() {
		
		
	    downloadDir = DOWNLOAD_DIR_V0;
	    sourceDir = SRC_DIR_V0;
	    
        File dir = new File(sourceDir);
        final File dndir = new File(downloadDir);
        
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +sourceDir);
		}

        if ( (!dndir.exists()) || (!dndir.isDirectory())) {
	        try {
				FileUtils.forceMkdir(dndir);
			} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
        }

		int counter = 0;
		
		String bucketName = null;
		bucketName = this.bucket_1.getName();
			
		// put files
		//
		for (File fi:dir.listFiles()) {
			
		    if (counter >=  (int) Math.round( Double.valueOf(Double.valueOf(getMax()).doubleValue()/2.0).doubleValue()))
				break;
			
			if (isElegible(fi)) {
				
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				
				try {
					
					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					logger.info( String.valueOf(testFiles.size() + " testAddObjects add -> " + fi.getName()));
					
					counter++; 
					sleep();
					
					/** display status every n seconds */
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.info( " testAddObjects add -> " + String.valueOf(testFiles.size()));
						showStatus = OffsetDateTime.now();
					}
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		logger.info( " testAddObjects add total -> " + String.valueOf(testFiles.size()));
		
		
		// -----------
		
		sub_index=0;
		testFiles.forEach( (k,v) -> {
		
			ObjectMetadata meta = null;
				
				try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
						 sub_index++;
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = downloadDir + File.separator + meta.fileName;
				
				if (new File(destFileName).exists())
					FileUtils.deleteQuietly(new File(destFileName));
				
				try {
						getClient().getObject(meta.bucketName, meta.objectName, destFileName);
						
				} catch (ODClientException | IOException e) {
						error(e);
				}
					try {
						String src_sha = v.getSrcFileSha256(0);
						String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
                            StringBuilder str  = new StringBuilder();
                            str.append("Error sha256 are not equal -> " + meta.bucketName+" / "+meta.objectName);
                            str.append(" | src -> " + v.getSrcFile(0).getAbsolutePath()           + "  " + String.valueOf(v.getSrcFile(0).length()/1000.0) + " kbytes");
                            str.append(" | dest -> "+ (new File(destFileName)).getAbsolutePath()  + "  " + String.valueOf(new File(destFileName).length()/1000.0) + " kbytes");
                            error(str.toString());
						}
						
						
						  /** display status every n seconds */
	                    if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
	                        logger.info( " testAddObjects check -> " + String.valueOf(sub_index));
	                        showStatus = OffsetDateTime.now();
	                    }
						
						
							
					} catch (NoSuchAlgorithmException | IOException e) {
						error(e);
					}
		});
	
	    logger.info("testAddObjects ok " + "  " + String.valueOf(testFiles.size()));
	    
		getMap().put("testAddObjects check" + " | " + String.valueOf(testFiles.size()), "ok");
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
	 * @param file
	 * @return
	 */



	
}






