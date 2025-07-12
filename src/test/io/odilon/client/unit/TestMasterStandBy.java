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
package io.odilon.client.unit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import io.odilon.client.ODClient;
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
 *  @author atolomei@novamens.com (Alejandro Tolomei) 	
 */
public class TestMasterStandBy extends BaseTest {
		
	private static final Logger logger = Logger.getLogger(TestMasterStandBy.class.getName());
	
	private String sourceDir;
	private String downloadDir;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	
	private int counter;	

	private String bucketTest = "dev-mstb-test";

	private boolean isMasterVersionControl = false;
	private boolean isStbyVersionControl = false;
	

	
	public TestMasterStandBy() {
	}
	
	
	@Override
	public void executeTest() {
		

		preCondition();
		
		testAddObjects();
		
		/** Version 0*/
		testReadObjectsFromStandBy(0);
		
			
		if (isMasterVersionControl && isStbyVersionControl) {
			putObjectNewVersion();
			
			// version 1 
			testReadObjectsFromStandBy(1);
			testVersions();
		}
			
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
			
        	
        	if (getStandByClient()==null) {
        		error("Standby client is null");
        	}
        	
        	String p=getStandByClient().ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				getMap().put("ping standby", "ok");
			}
		} catch (Exception e)	{
			error(e.getClass().getName() + " | " + e.getMessage());
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
       	
       	
        File v2dir = new File(getSourceV1Dir());
		FileUtils.deleteQuietly(v2dir);
       	try {
			FileUtils.forceMkdir(v2dir);
		} catch (IOException e) { 
			error(e);
		}
		

       	
       	File tmpdirv1 = new File(DOWNLOAD_DIR_V1);
        
        if ( (tmpdirv1.exists()) && (tmpdirv1.isDirectory())) { 
        	try {
				FileUtils.forceDelete(tmpdirv1);
			} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(tmpdirv1);
				
		} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
		}

       	
       	
       	
       	try {
	       	
       		isMasterVersionControl = getClient().isVersionControl();
			isStbyVersionControl = getStandByClient().isVersionControl();
			
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
			return false;
		}
		
        
        /**
         * create bucket in Master and StandBy 
         */
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
	
	
	/**
	 * @return
	 */
	public boolean testVersions() {
	
		testFiles.forEach( (k,v) -> {
			
			ObjectMetadata metaMaster = null;
			ObjectMetadata metaStandBy = null;
			
			try {
					metaMaster = getClient().getObjectMetadata(v.bucketName, v.objectName);
					
			} catch (ODClientException e) {
					error(e);
			}
			
			try {
				 metaStandBy = getStandByClient().getObjectMetadata(v.bucketName, v.objectName);
				
			} catch (ODClientException e) {
					error(e);
			}
				
			if (metaMaster.version!=metaStandBy.version) 
				error("version error " + metaMaster.bucketName + " o:  " + metaMaster.objectName +" -> vMaster" + String.valueOf(metaMaster.version) + " vStandby:" +  String.valueOf(metaStandBy.version));
												
			if (metaMaster.length!=metaStandBy.length)   
				error("length error " + metaMaster.bucketName + " o:  " + metaMaster.objectName +" -> lengthMaster" + String.valueOf(metaMaster.length) + " vStandby:" +  String.valueOf(metaStandBy.length));
		});
		
		getMap().put("testVersions", "ok");
		return true;
		
	}

	
	/**
	 * @return
	 */
	public boolean testReadObjectsFromStandBy(int ver) {
		
		
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
						logger.info( String.valueOf(" testReadObjectsFromStandBy-> " + destFileName));
						
				} catch (ODClientException | IOException e) {
						error(e);
				}
				
				TestFile t_file = testFiles.get(meta.bucketName+"-"+meta.objectName);
				
				if (t_file!=null) {
					try {
					
						String src_sha = t_file.getSrcFileSha256(meta.version);
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
						error("Test Standby server file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
		
		});
		
		getMap().put("testReadObjectsFromStandBy" + " | (" + String.valueOf(testFiles.size())+") | version: " + String.valueOf(ver), "ok");
		return true;
	}
	
	
	
	
	
	public boolean testAddObjects() {
		
	   downloadDir = super.getDownloadDirHeadVersion();
	   sourceDir =  getSourceDir();
        
	   final File dir = new File(sourceDir);
	   
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
					logger.info( String.valueOf(testFiles.size() + " testAddObjects -> " + fi.getName()));
					counter++; 
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		
		/**
		 * check upload worked
		 */
		
		
		testFiles.forEach( (k,v) -> {
		
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
				
				TestFile t_file = testFiles.get(meta.bucketName+"-"+meta.objectName);
				
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
						logger.error(e);
						error(e);
					}
				}
				else {
						error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
		
		});
	
		getMap().put("testAddObjects" + " | (" + String.valueOf(testFiles.size())+")", "ok");
		return true;
	
	}	
		
	
	
	

	
	
	private boolean putObjectNewVersion() {
		
		counter = 0;
		 
		testFiles.forEach((k,v) -> 
		{
			String srcname=v.getSrcFile(0).getName();

			String name=FilenameUtils.getBaseName(srcname);
			String ext=FilenameUtils.getExtension(srcname);
			
			String nameNewVersion= getSourceV1Dir() + File.separator + name +"-v1" + "."+ext;
			
			try {
				if ((new File(nameNewVersion)).exists())
					FileUtils.forceDelete(new File(nameNewVersion));
					
			} catch (IOException e) {
				error(e.getClass().getName()+ " - can not delete existing new version locally");
			}
			
			try {
				Files.copy( v.getSrcFile(0).toPath(), 
							new File(nameNewVersion).toPath(), 
							StandardCopyOption.REPLACE_EXISTING );
			} catch (IOException e) {
				error(e.getClass().getName() + " - can copy version locally");
			}
			
			try {
				
				/**upload new version */
				
				getClient().putObject(v.bucketName, v.objectName, new File(nameNewVersion));

				counter++;
				testFiles.get(v.bucketName+"-"+v.objectName).addSrcFileVersion(new File(nameNewVersion));

				sleep();

				
			} catch (ODClientException e) {
				logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				error(e);
			}
			
		}); 

		logger.info( "putObjectNewVersion total -> " + String.valueOf(counter));
		
		
		boolean success = validateSet(testFiles);
		
		if (success)
			logger.debug("putObjectNewVersion ok");
		else
			logger.debug("putObjectNewVersion error");

		getMap().put("putObjectNewVersion", success?"ok":"error");
		
		return success;
		
	}

	
	private boolean validateSet(Map<String, TestFile> mv) {
		
		mv.forEach((k,v) -> {
			
			ObjectMetadata meta = null;
			
			try {
					 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
					
			} catch (ODClientException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
				
			String destFileName = DOWNLOAD_DIR_V1 + File.separator + meta.fileName;
			
			try {
				if ((new File(destFileName)).exists())
					FileUtils.forceDelete( new File(destFileName));
				
			} catch (IOException e) {
				error(e.getClass().getName() + " | FileUtils.forceDelete( new File(destFileName));");
			}
			
			try {
					getClient().getObject(meta.bucketName, meta.objectName, destFileName);
					
			} catch (ODClientException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
			
			try {
				
					String src_sha = v.getSrcFileSha256(meta.version);
					String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
					
					if (!src_sha.equals(new_sha)) {
						logger.error("sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						error("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
					}
					
			} catch (NoSuchAlgorithmException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
		});
						
		
		logger.debug("validateSet", "ok");
		getMap().put("validateSet", "ok");
		
		return true;
	}
	
	


}
