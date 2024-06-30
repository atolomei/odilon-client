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
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public class TestDeleteObject extends BaseTest {
			
	private static Logger logger = Logger.getLogger(TestDeleteObject.class.getName());

	
	static final int BUFFER_SIZE = 4096;
	
	static final int MAX = 10;
	static final long MAX_LENGTH = 100 * 10000; // 1 MB
	
	private int index = 0;
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	private OffsetDateTime showStatus = OffsetDateTime.now();	
	
	
	public TestDeleteObject() {
	}
		 
	
	public void executeTest() {
		
		bucket_1 = createTestDeleteBucket();
		
		index = 0;
		testAddObjects();
		
		index = 0;
		testDeleteObjects();
		
		
		index = 0;
		testDeleteNonExistentObjects();
		
		index = 0;
		removeTestBucket();

		//index = 0;
		//testDeleteAllObjects();
		
		showResults();

	}

	public boolean testDeleteNonExistentObjects() {
		
		
		try {
			
			sleep();
			
			/** display status every 4 seconds or so */
			if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
				logger.info( "testFiles -> " + String.valueOf(testFiles.size()));
				showStatus = OffsetDateTime.now();
			}

			
			getClient().deleteObject(this.bucket_1.getName(), randomString(12));
			error("should have thrown exception - delete object non existent");
			return false;
			
		} catch (ODClientException e) {
			logger.debug("This exception is ok -> " + e.toString());
			
			logger.debug("testDeleteNonExistentObjects", "ok");
			getMap().put("testDeleteNonExistentObjects", "ok");
			
			return true;
		}
		
		
		
		
	}
	

	public boolean testDeleteAllObjects() {
		
		
		try {
			
			
			getClient().listBuckets().forEach( bucket ->  {
				
				try {

					ResultSet<Item<ObjectMetadata>>  rs = getClient().listObjects(bucket);

					while (rs.hasNext()) {
						Item<ObjectMetadata> item = rs.next();
						if (item.isOk()) {
							getClient().deleteObject(item.getObject().bucketName, item.getObject().objectName);				
						}
						else {
							logger.debug(item.getErrorString());
						}
				
						/** display status every 4 seconds or so */
						if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
							logger.info( "testDeleteAllObjects -> " + String.valueOf(getCounter()));
							showStatus = OffsetDateTime.now();
						}
					}

				} catch (ODClientException e) {
					logger.error(e);
				}
			});

			logger.debug("testDeleteAllObjects", "ok");
			getMap().put("testDeleteAllObjects", "ok");

			return true;
			
		} catch (ODClientException e) {
			error(e);
			return false;
		}
		
	}
	
	

	/**
	 * 
	 * @return
	 */
	public Bucket createTestDeleteBucket() {
		
		String bucketName = "test-delete";
		
		try {
			if (!getClient().existsBucket(bucketName)) {
					getClient().createBucket(bucketName);
					logger.debug("createTestBucket -> ok");
					getMap().put("createTestBucket", "ok");
			}
			return getClient().getBucket(bucketName);
			
		} catch (ODClientException e) {
			error(e);
			return null;
		}
		
		
			
	}
	
	public boolean testDeleteObjects() {
	
		for( TestFile tf: testFiles.values()) {
			
			try {
				
				if (getClient().existsObject(tf.bucketName, tf.objectName)) {
					
					getClient().deleteObject(tf.bucketName, tf.objectName);
					
					if (getClient().existsObject(tf.bucketName, tf.objectName)) {
						error("should not exist ->" + tf.bucketName + " | " + tf.objectName);
					}
					
					sleep();

					/** display status every 4 seconds or so */
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.info( "testDeleteObjects -> " + String.valueOf(getCounter()));
						showStatus = OffsetDateTime.now();
					}
					
					
				}
			} catch (ODClientException | IOException e) {
				logger.error(e);	
				error(e);
			}
			
		}
		
		logger.debug("testDeleteObjects", "ok");
		getMap().put("testDeleteObjects", "ok");
		
		return true;
		
		
		
	}
	
	/**
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
		
		
		for (File fi:dir.listFiles()) {
			
			if (counter == getMax())
				break;
			
			if (!fi.isDirectory() && (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName()) || FSUtil.isZip(fi.getName())) && (fi.length()<MAX_LENGTH)) {
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				try {
					
					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName+"-"+objectName, new TestFile(fi, bucketName, objectName));
					counter++; 
					
					sleep();

					/** display status every 4 seconds or so */
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.info( "testAddObjects -> " + String.valueOf(getCounter()));
						showStatus = OffsetDateTime.now();
					}
					
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));

				}
			}
		}
		
		
		logger.info( "testAddObjects -> Total:  " + String.valueOf(testFiles.size()));
		
		
		testFiles.forEach( (k,v) -> {
		ObjectMetadata meta = null;
		
		try {
				 meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
				
		} catch (ODClientException e) {
				error(e);
		}
			
		String destFileName = DOWNLOAD_DIR_V0 + File.separator + meta.fileName;
		
		try {
				getClient().getObject(meta.bucketName, meta.objectName, destFileName);
				
		} catch (ODClientException | IOException e) {
				error(e);
		}
		
		TestFile t_file=testFiles.get(meta.bucketName+"-"+meta.objectName);
		
		if (t_file!=null) {
			
			try {
				String src_sha = t_file. getSrcFileSha256(0);
				String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));
				
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
	
	logger.debug("testAddObjects", "ok");
	getMap().put("testAddObjects", "ok");
	return true;
	}

	


protected int getCounter() {
	return index++;
}


}
