package io.odilon.test.stress;

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

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

public class TestManyUploads extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(TestManyUploads.class.getName());

	private String sourceDir;
	private String downloadDir;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	
	private OffsetDateTime showStatus = OffsetDateTime.now();
	
	public TestManyUploads() {
		

		String max = System.getProperty("manymax");
		if (max!=null)
			setMax(Integer.valueOf(max.trim()));
		else
			setMax(4000);
	}
	

	
	@Override
	public void executeTest() {
		
		preCondition();

		if (!testAddObjectsStream("java http"))
			error("testAddObjectsStream java http");
		
	
		 showResults();

	}

	
	@Override
	protected int getMax() {
		return super.getMax();
	}
	
	/**
	 */
	public boolean testAddObjectsStream(String version) {
		
    	
	    downloadDir = super.getDownloadDirHeadVersion();
	    sourceDir =  getSourceDir();
	    
	    
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
        
        	
		int iteration = 0;
		
		String bucketName = this.bucket_1.getName();
		
		boolean hasHope = true;
		
		long processstart = System.currentTimeMillis();

		
		while ((testFiles.size()<getMax()) && hasHope) {
			
				hasHope = false;
		
				Map<String, TestFile> iterationTestFiles = new HashMap<String, TestFile>();
				
				long start = System.currentTimeMillis();
				
				
				for (File file: dir.listFiles()) {
					
					if (testFiles.size() >= getMax())
						break;
					
					if (isElegible(file)) {
						
						String objectName = FSUtil.getBaseName(file.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*10000))).intValue());;
		
						try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
							
							getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.empty());
							
							testFiles.put(bucketName+"-"+objectName, new TestFile(file, bucketName, objectName));
							iterationTestFiles.put(bucketName+"-"+objectName, new TestFile(file, bucketName, objectName));
							
							
							logger.info( String.valueOf(testFiles.size() + " testAddObjectsStream -> " + file.getName()));
							hasHope = true;
							
							sleep();
							
							if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
								logger.info( "testAddObjectsStream -> " + String.valueOf(testFiles.size()));
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
				
				logger.info( "testAddObjectsStream total -> " + String.valueOf(testFiles.size()));
				
				{
				
				iterationTestFiles.forEach((k,v) -> {
					
					ObjectMetadata meta = null;
					try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
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
					
					TestFile t_file=iterationTestFiles.get(meta.bucketName+"-"+meta.objectName);
					
					if (t_file!=null) {
						
						try {
						
							String src_sha = t_file.getSrcFileSha256(0);
							String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
							
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
				}
																
				logger.info( "testAddObjectsStream check iteration correct -> " + String.valueOf(iteration));
				logger.info( "testAddObjectsStream iteration -> " + String.valueOf(iteration) + " | "+ String.valueOf(iterationTestFiles.size()) + " | " + String.format("%12d ms", System.currentTimeMillis()-start));
				
				iteration++; 
				
		}
		
		
		
		logger.info( "testAddObjectsStream put total -> " + String.valueOf(testFiles.size()) + " | "+ String.format("%12d ms", System.currentTimeMillis()-processstart));
		
		
		
		getMap().put("testAddObjectsStream " + version + " | " + String.valueOf(testFiles.size()), "ok");
		
		return true;
	}

	
	/**
	 * 
	 * 
	 */
	public boolean preCondition() {

		{
	        File dir = new File( getSourceDir());
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +getSourceDir());
			}
		}
		

		{
	        File dir = new File( getSourceV1Dir() );
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " + getSourceV1Dir());
			}
		}

		
		{
	        File dir = new File(getSourceV2Dir());
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +  getSourceV2Dir());
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
        File tmpdir = new File(super.getDownloadDirHeadVersion());
        
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

        String bucketTest = "test-many";
        
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
