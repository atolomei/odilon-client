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
package io.odilon.client.unit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
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
import io.odilon.util.OdilonFileUtils;

/**
 * 
 * Put Object
 * Get Object
 * Get PresignedUrl
 * Remove Object
 *
 */
public class TestObjectPutGetShort extends BaseTest {
			
			
	private static final Logger logger = Logger.getLogger(TestObjectPutGetShort.class.getName());
						
	
	static final String TEMP_DIR = "c:"+File.separator+"temp";
	static final String DOWNLOAD_DIR = "c:"+File.separator+ "temp" + File.separator + "download";
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private final File saveDir = new File(DOWNLOAD_DIR);
	
	public TestObjectPutGetShort() {
	}

	
	@Test	
	public void executeTest() {

		preCondition();
		
		if (!testAddObjectsStream("java http"))
		error("testAddObjectsStream java http");
		showResults();
	}
	
	/**
	 */
	public boolean testAddObjectsStream(String version) {
		
	    Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
        				
	    final File dir = new File(TEMP_DIR);
	    
        if ((!dir.exists()) || (!dir.isDirectory()))  
			error("Dir not exists or the File is not Dir -> " +TEMP_DIR);
		
        if ( (!saveDir.exists()) || (!saveDir.isDirectory())) {
	        try {
				FileUtils.forceMkdir(saveDir);
			} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
        }
        
        try {

			int counter = 0;
			String bucketName = this.bucket_1.getName();
			for (File fi:dir.listFiles()) {
				if (counter == getMax())
					break;
				
				if (isElegible(fi)) {
					String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*10000))).intValue());;
					InputStream inputStream = null;
					
					try {
						inputStream = new BufferedInputStream(new FileInputStream(fi));
						
					} catch (FileNotFoundException e) {
						error(e);
					}
	
					try {
						getClient().putObjectStream(
															bucketName, 
															objectName, 
															inputStream, 
															Optional.of(fi.getName()),
															Optional.empty()
													);
						
						testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
						counter++;
						
					} catch (ODClientException e) {
						error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
					}
					finally {
						if (inputStream!=null) {
							try {
								inputStream .close();
							} catch (IOException e) {
								error(e);
							}
						}
					}		
				}
			}
			
			testFiles.forEach((k,v) -> {
					
				ObjectMetadata meta = null;
				try {
					 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
				} catch (ODClientException e) {
						logger.error(e);
						System.exit(1);
				}
					
				String destFileName = DOWNLOAD_DIR + File.separator + meta.fileName;
				
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
						String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							StringBuilder str  = new StringBuilder();
							str.append("Error sha256 are not equal -> " + meta.bucketName+" / "+meta.objectName);
							str.append(" | src -> " + String.valueOf(t_file.getSrcFile(0).length()) + "bytes");
							str.append(" | dest -> " + String.valueOf(new File(destFileName).length()) + "bytes");
							error(str.toString());
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
						throw new RuntimeException(e);
					}
				}
				else {
					error("Test file does not exist -> " + meta.bucketName+"-"+meta.objectName);
				}
			});
			
			
			getMap().put("testAddObjectsStream " + version + " | " + String.valueOf(testFiles.size()), "ok");
				
			return true;
				
        }
		finally {
				//try {
				//	FileUtils.forceDelete(saveDir);
				//} catch (IOException e) {
				//	logger.error(e);
				//	error(e.getClass().getName() + " | " + e.getMessage());
				//}
			}
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
						String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
						
						if (!src_sha.equals(new_sha)) {
							throw new RuntimeException("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						}
							
					} catch (NoSuchAlgorithmException | IOException e) {
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

	/**
	 * 
	 * 
	 */
	public boolean preCondition() {

        File dir = new File(TEMP_DIR);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +TEMP_DIR);
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
	



}






