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


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class TestObjectPutVersionSimple extends BaseTest {

	
	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	
	
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private Bucket bucket_1;

	int counter = 0;
	
	OffsetDateTime showStatus = OffsetDateTime.now();

	
	/**
	 * 
	 * uploada n files and then a new version for each of them
	 * 
	 */
	public TestObjectPutVersionSimple() {
	}
	

	@Override
	public void executeTest() {
		
		if (!preCondition()) 
			error("preCondition");
		
		if (!putObject("putObject"))
			error("putObject");
		
		if (!putObjectNewVersion("putObjectNewVersion"))
			error("putObjectNewVersion");
		
		showResults();

	}

	public boolean preCondition() {

		try {
			String p=ping();
			
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				logger.debug("ping -> ok");
				getMap().put("ping", "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		
        File dir = new File( super.getSourceDir());
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " + getSourceDir());
		}


        File v2dir = new File( getSourceV1Dir() );
        
    	
		FileUtils.deleteQuietly(v2dir);
       	try {
			FileUtils.forceMkdir(v2dir);
		} catch (IOException e) {
			error(e);
		}
		
        
        File tmpdir = new File(super.getDownloadDirHeadVersion());
        
        if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
        	try {
				FileUtils.forceDelete(tmpdir);
			} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(tmpdir);
				
		} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
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
			
			if (!getClient().existsBucket("test-put-version")) {
				getClient().createBucket("test-put-version");
			}
			
			bucket_1 = getClient().getBucket("test-put-version");
			
		} catch (ODClientException e) {
			logger.error(e);
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		
		
		logger.debug("precondition -> ok");
		getMap().put("precondition", "ok");
		
		return true;
	}

	
	
	
	/**
	 * 
	 * 
	 */
	private boolean putObject(String mname) {

		logger.debug("Starting " + mname);
		
		File dir = new File(getSourceDir());
		
		 counter = 0;
		
		String bucketName = bucket_1.getName();
		
		// put files
		for (File fi:dir.listFiles()) {

			if (counter == getMax())
				break;
			
			if (!fi.isDirectory() && (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName())) && (fi.length()<getMaxLength())) {
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				objectName = getClient().normalizeObjectName(objectName);

				try {
					getClient().putObject(bucketName, objectName, fi);
					counter++;
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					
					sleep();
					
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.info( mname + " -> " + String.valueOf(getCounter()));
						showStatus = OffsetDateTime.now();
					}
					
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		logger.info( mname + " total -> " + String.valueOf(getCounter()));
		getMap().put(mname, "ok "  + String.valueOf(counter));
		
		return true;
		
	}
	
	
	
	protected int getCounter() {
		return counter;
	}
	
	
	
	private boolean putObjectNewVersion(String mname) {
		
		logger.debug("Starting " + mname);

		
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

				if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
					logger.info( mname + " -> " + String.valueOf(getCounter()));
					showStatus = OffsetDateTime.now();
				}
				
				
			} catch (ODClientException e) {
				logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				error(e);
			}
			
		}); 

		logger.info( mname + " total -> " + String.valueOf(getCounter()));
		
		
		boolean success = validateSet(testFiles);
		
		if (success)
			logger.debug(mname +" ok");
		else
			logger.debug(mname + " error");

		getMap().put(mname, success?"ok":"error");
		
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
			
			
			if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
				logger.info( "validateSet -> " + String.valueOf(getCounter()));
				showStatus = OffsetDateTime.now();
			}
		});
						
		
		logger.debug("validateSet", "ok");
		getMap().put("validateSet", "ok");
		
		return true;
	}
	

}
