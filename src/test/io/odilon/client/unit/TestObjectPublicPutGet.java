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
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

/**
 * 
 * Put Object Get Object Get PresignedUrl Remove Object
 *
 *
 */
public class TestObjectPublicPutGet extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPublicPutGet.class.getName());

	private String sourceDir;
	private String downloadDir;

	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

	private OffsetDateTime showStatus = OffsetDateTime.now();
	private int sub_index = 0;

	public TestObjectPublicPutGet() {
	}

	@Test
	public void executeTest() {

		preCondition();

		String change = "change urls to public access";
		if (!changeToPublicAccess(change))
			error(change);
		
		String putGetResourcesWithPublicAccess = "put resources public access";
		if (!testPutGuetResourcesWithPublicAccess(putGetResourcesWithPublicAccess))
			error(putGetResourcesWithPublicAccess);

		
		String getPublicUrls = "get public url";
		if (!testGetUrlResources(getPublicUrls))
			error(getPublicUrls);

		showResults();
	
	}
	
	
	public boolean changeToPublicAccess(String name) {
		
		 ;
		
		try {
			ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
			
		 
			int total = 0;
			
			while (rs.hasNext()  && total <  getMaxFilesToTest() ) {
				Item<ObjectMetadata> item = rs.next();
				if (item.isOk()) {
					ObjectMetadata meta = item.getObject();
				
					if (!meta.isPublicAccess() ) {
						
							total++;
					
							getClient().setPublicAccess(meta.bucketName,  meta.objectName, true);
							logger.debug(meta.bucketName + " / " + meta.objectName + " set to publicAccess = true");

							ObjectMetadata metaUpdated = getClient().getObjectMetadata(meta.bucketName,  meta.objectName);
							
							if (!metaUpdated.isPublicAccess())
								error(metaUpdated.bucketName + " / " + meta.objectName + " -> should be publicAccess=true and it is false");
							else {
								String url = getClient().getPublicObjectUrl(metaUpdated.bucketName, meta.objectName);
								logger.debug(metaUpdated.bucketName + " / " + meta.objectName + " -> " + url);
							}
								
						}
					}
			}
			getMap().put( name +" -> " + String.valueOf(total), "ok");
		
		} catch (ODClientException  e) {
			error(e);
		}
		
		return true;
	}
	
	
	public boolean testPutGuetResourcesWithPublicAccess(String name) {
		
		Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

		

		
		int counter = 0;
		String bucketName = this.bucket_1.getName();

		int max = getMaxFilesToTest();

		logger.info("Trying to upload -> " + String.valueOf(max) + " files");

		final File dir = new File(this.sourceDir);
		
		for (File file : dir.listFiles()) {

			if (counter >= max)
				break;

			if (isElegible(file)) {

				String objectName = FSUtil.getBaseName(file.getName()) + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue());
				 
				objectName = getClient().normalizeObjectName(objectName);

				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {

					List<String> customTags = new ArrayList<String>();
					customTags.add(String.valueOf(counter));

					getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.empty(), Optional.empty(), Optional.ofNullable(customTags), Optional.of(Boolean.TRUE));
					testFiles.put(bucketName + "-" + objectName, new TestFile(file, bucketName, objectName));
					counter++;

					sleep();

					if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
						logger.info("testAddObjectsStream add -> " + String.valueOf(testFiles.size()));
						showStatus = OffsetDateTime.now();
					}

				} catch (ODClientException e) {
					error("Http status " + String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
				} catch (FileNotFoundException e1) {
					error(e1);
				} catch (IOException e2) {
					error(e2);
				}

			}
		}

		logger.info( name + " add total -> " + String.valueOf(testFiles.size()));

		sub_index = 0;

		
		{
			try {
				ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
				counter = 0;
				int total = 0;
				while (rs.hasNext() && total < testFiles.size()) {
					Item<ObjectMetadata> item = rs.next();
					if (item.isOk()) {
						ObjectMetadata meta = item.getObject();
						if (testFiles.containsKey(meta.bucketName + "-" + meta.objectName)) {
							total++;
							if (meta.isPublicAccess() ) {
								logger.debug(meta.bucketName + " / " + meta.objectName + " ok ");
							}
							else
								error(meta.bucketName + " / " + meta.objectName + " should be public access and it is not");
						}
					}
				}
				getMap().put( name +" -> " + String.valueOf(total), "ok");
			
			} catch (ODClientException  e) {
				error(e);
			}
		}

		
		
		return true;
		
	}
	
	/**
	 * 
	 */
	public boolean testGetUrlResources(String name) {
		
		try {
			
			getClient().listBuckets().forEach( bucket -> {
				
				ResultSet<Item<ObjectMetadata>> rs;
				try {
					rs = getClient().listObjects(bucket.getName());
				
					int total = 0;
					while (rs.hasNext() && total++ < getMaxFilesToTest()) {
						Item<ObjectMetadata> item = rs.next();
						ObjectMetadata meta = item.getObject();
						if (meta.isPublicAccess() ) {
						String str = getClient().getPublicObjectUrl(meta.bucketName, meta.objectName);
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> " + str);
						total++;
						}
					}
				
				} catch (ODClientException e) {
					error(e);
				}
			});
		} catch (ODClientException e) {
			error(e);
		}
		
		getMap().put(name,  "ok");
		return true;
	}
	
	
	/**
	 * 
	 * 
	 * 
	 * 
	 */
	public boolean testAddPublicResources(String version) {

		Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

		this.downloadDir = super.getDownloadDirHeadVersion();
		final File dndir = new File(this.downloadDir);

		this.sourceDir = super.getSourceDir();
		final File dir = new File(this.sourceDir);

		if ((!dir.exists()) || (!dir.isDirectory())) {
			try {
				FileUtils.forceMkdir(dir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}

		}

		if ((!dir.exists()) || (!dir.isDirectory()))
			error("Dir not exists or the File is not Dir -> " + sourceDir);

		if ((!dndir.exists()) || (!dndir.isDirectory())) {
			try {
				FileUtils.forceMkdir(dndir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}

		int counter = 0;
		String bucketName = this.bucket_1.getName();

		int max = getMaxFilesToTest();

		logger.info("Trying to upload -> " + String.valueOf(max) + " files");

		for (File file : dir.listFiles()) {

			if (counter >= max)
				break;

			if (isElegible(file)) {

				String objectName = FSUtil.getBaseName(file.getName()) + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue());
				 
				objectName = getClient().normalizeObjectName(objectName);

				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {

					List<String> customTags = new ArrayList<String>();
					customTags.add(String.valueOf(counter));

					getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.empty(), Optional.empty(), Optional.ofNullable(customTags), Optional.of( Boolean.TRUE));
					testFiles.put(bucketName + "-" + objectName, new TestFile(file, bucketName, objectName));
					counter++;

					sleep();

					if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
						logger.info("testAddObjectsStream add -> " + String.valueOf(testFiles.size()));
						showStatus = OffsetDateTime.now();
					}

				} catch (ODClientException e) {
					error("Http status " + String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
				} catch (FileNotFoundException e1) {
					error(e1);
				} catch (IOException e2) {
					error(e2);
				}

			}
		}

		logger.info("TestObjectPublicPutGet add total -> " + String.valueOf(testFiles.size()));

		sub_index = 0;

		
		{
			try {
		
				List<String> list = new ArrayList<String>();
				
				ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
				counter = 0;
				int total = 0;
				while (rs.hasNext() && total++ < getMaxFilesToTest()) {
					Item<ObjectMetadata> item = rs.next();
					if (item.isOk()) {
						ObjectMetadata meta = item.getObject();
						if (meta.isPublicAccess() ) {
						String str = getClient().getPublicObjectUrl(meta.bucketName, meta.objectName);
						list.add(str);
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> " + str);
						total++;
						}
					}
				}

				getMap().put("public test  -> " + String.valueOf(total), "ok");
			
			} catch (ODClientException  e) {
				error(e);
			}
		}

		
		/**
		testFiles.forEach((k, v) -> {

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
					StringBuilder str = new StringBuilder();
					str.append("testAddObjectsStream Error sha256 are not equal -> " + meta.bucketName + " / " + meta.objectName);
					str.append(" | src -> " + v.getSrcFile(0).getAbsolutePath() + "  " + String.valueOf(v.getSrcFile(0).length() / 1000.0) + " kbytes");
					str.append(" | dest -> " + (new File(destFileName)).getAbsolutePath() + "  " + String.valueOf(new File(destFileName).length() / 1000.0) + " kbytes");
					error(str.toString());
				}

			} catch (NoSuchAlgorithmException | IOException e) {
				logger.error(e);
				error(e);
			}

			if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
				logger.info("testAddObjectsStream checking -> " + String.valueOf(sub_index));
				showStatus = OffsetDateTime.now();
			}
		});

		logger.info("testAddObjectsStream -> ok " + String.valueOf(testFiles.size()));

		getMap().put("testAddObjectsStream " + version + " | " + String.valueOf(testFiles.size()), "ok");
*/
		
		return true;
	}

	/**
	 * 
	 * 
	 */
	public boolean preCondition() {

		{
			this.downloadDir = super.getDownloadDirHeadVersion();
			final File dndir = new File(this.downloadDir);
	
			this.sourceDir = super.getSourceDir();
			final File dir = new File(this.sourceDir);
	
			if ((!dir.exists()) || (!dir.isDirectory())) {
				try {
					FileUtils.forceMkdir(dir);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
	
			}
	
			if ((!dir.exists()) || (!dir.isDirectory()))
				error("Dir not exists or the File is not Dir -> " + sourceDir);
	
			if ((!dndir.exists()) || (!dndir.isDirectory())) {
				try {
					FileUtils.forceMkdir(dndir);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}
		}
		
		
		{
			File dir = new File(getSourceDir());

			if ((!dir.exists()) || (!dir.isDirectory())) {
				error("Dir not exists or the File is not Dir -> " + getSourceDir());
			}
		}

		{
			File dir = new File(getSourceV1Dir());

			if ((!dir.exists()) || (!dir.isDirectory())) {
				error("Dir not exists or the File is not Dir -> " + getSourceV1Dir());
			}
		}

		{
			File dir = new File(getSourceV2Dir());

			if ((!dir.exists()) || (!dir.isDirectory())) {
				error("Dir not exists or the File is not Dir -> " + getSourceV2Dir());
			}
		}

		try {
			String p = ping();
			if (p == null || !p.equals("ok"))
				error("ping  -> " + p != null ? p : "null");
			else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		{
			File tmpdir = new File(super.getDownloadDirHeadVersion());

			if ((tmpdir.exists()) && (tmpdir.isDirectory())) {
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

			if ((tmpdir.exists()) && (tmpdir.isDirectory())) {
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

			if ((tmpdir.exists()) && (tmpdir.isDirectory())) {
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
