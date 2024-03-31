package io.odilon.client.unit;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.ODFileUtils;

public class TestMasterStandBy extends BaseTest {
		
	private static final Logger logger = Logger.getLogger(TestMasterStandBy.class.getName());
	
	
	static final String TEMP_DIR 					 = "d:"+File.separator+"test-files";
	static final String DOWNLOAD_DIR				 = "d:"+File.separator+ "test-files-download";
	static final String DOWNLOAD_STAND_BY_DIR 		 = "d:"+File.separator+ "test-files-standby-download";
	
	
	static final int BUFFER_SIZE = 4096;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private String standByEndpoint = "http://localhost";
	private int standByPort = 9211;
	
	private String standByAccessKey = "odilon";
	private String standBySecretKey = "odilon";
	
	private ODClient standByClient;
	

	public TestMasterStandBy() {
	}
	
	@Override
	public void executeTest() {
		
		connectStandBy(); 
		preCondition();
		
		testAddObjects();
		testReadObjectsFromStandBy();		
		
		showResults();
	}



	public boolean preCondition() {

        try {
			String p=ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				getMap().put("ping master", "ok");
			}
		} catch (Exception e)	{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
        

        try {
			String p=getStandByClient().ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				getMap().put("ping standby", "ok");
			}
		} catch (Exception e)	{
			error(e.getClass().getName() + " | " + e.getMessage());
		}

        
        
        File dir = new File(TEMP_DIR);
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +TEMP_DIR);
		}
        

        {
        File tmpdir = new File(DOWNLOAD_DIR);
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
        File tmpdir = new File(DOWNLOAD_STAND_BY_DIR);
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

       	
        
        /**
         * create bucket in Master and StandBy 
         */
        String bucketTest = "dev-test";
        
        try {	
			if (!getClient().existsBucket(bucketTest)) {
				getClient().createBucket(bucketTest);
			}
			
			bucket_1 = getClient().getBucket(bucketTest);
			return true;
			
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
			return false;
		}
        
	}
	

	public boolean testReadObjectsFromStandBy() {
		
		
		testFiles.forEach( (k,v) -> {
			
			ObjectMetadata meta = null;
				
				try {
						 meta = getStandByClient().getObjectMetadata(v.bucketName, v.objectName);
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = DOWNLOAD_STAND_BY_DIR + File.separator + meta.fileName;
				
				try {
						getStandByClient().getObject(meta.bucketName, meta.objectName, destFileName);
						
				} catch (ODClientException | IOException e) {
						error(e);
				}
				
				TestFile t_file = testFiles.get(meta.bucketName+"-"+meta.objectName);
				
				if (t_file!=null) {
					
					try {
					
						String src_sha = t_file.getSrcFileSha256(0);
						String new_sha = ODFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							throw new RuntimeException("Standby server Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
						error(e);
					}
				}
				else {
						error("Test Standby server file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
		
		});
		
		getMap().put("testReadObjectsFromStandBy" + " | (" + String.valueOf(testFiles.size())+")", "ok");
		return true;
	}
	
	

	public boolean testAddObjects() {
		
        File dir = new File(TEMP_DIR);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +TEMP_DIR);
		}
        
		int counter = 0;
		
		String bucketName = null;
		bucketName = this.bucket_1.getName();
			
		// put files
		//
		for (File fi:dir.listFiles()) {
			
			if (counter == getMax())
				break;
			
			if (isElegible(fi)) {
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				try {
					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					counter++; 
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		
		// -----------
		
		testFiles.forEach( (k,v) -> {
		
			ObjectMetadata meta = null;
				
				try {
						 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
						
				} catch (ODClientException e) {
						error(e);
				}
					
				String destFileName = DOWNLOAD_DIR + File.separator + meta.fileName;
				
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
	
		getMap().put("testAddObjects" + " | (" + String.valueOf(testFiles.size())+")", "ok");
		return true;
	
	}	
		
	private ODClient getStandByClient() {
		return standByClient;
	}

	private void connectStandBy() {
		 try {
				this.standByClient = new ODClient(standByEndpoint, standByPort, standByAccessKey, standBySecretKey);
				logger.debug(standByClient.toString());
		        
			} catch (Exception e) {
				error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
			}

	}

}
