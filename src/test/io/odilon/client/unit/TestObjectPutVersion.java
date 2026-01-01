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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.hamcrest.core.Is;

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
 */
public class TestObjectPutVersion extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutVersion.class.getName());

	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

	private Bucket bucket_1;

	int counter = 0;

	OffsetDateTime showStatus = OffsetDateTime.now();

	/**
	 * 
	 * uploada n files and then a new version for each of them
	 * 
	 */
	public TestObjectPutVersion() {
	}

	String baseDir = "/opt/dev-data/test-files/test-version";
	String downloadDir = "/opt/dev-data/test-files/test-version/download";

	String fileNameV0 = "tolomei.jpg";
	String fileNameV1 = "tolomei-v1.jpg";

	@Override
	public void executeTest() {

		if (!preCondition())
			error("preCondition");

		if (!putObject("putObjectVersion"))
			error("putObject");

		showResults();

	}

	public boolean preCondition() {

		try {
			String p = ping();

			if (p == null || !p.equals("ok"))
				error("ping  -> " + p != null ? p : "null");
			else {
				logger.debug("ping -> ok");
				getMap().put("ping", "ok");
			}
		} catch (Exception e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		File dir = new File(baseDir);
		if ((!dir.exists()) || (!dir.isDirectory())) {
			error("Dir not exists or the File is not Dir -> " + baseDir);
		}

		File dndir = new File(downloadDir);
		if ((!dndir.exists()) || (!dndir.isDirectory())) {
			error("Download dir not exists or the File is not Dir -> " + downloadDir);
		}

		try {
			FileUtils.cleanDirectory(dndir);
		} catch (IOException e) {
			error("Download dir can not be cleaned up -> " + downloadDir);
		}

		File file = new File(baseDir, fileNameV0);
		if ((!file.exists()) || (file.isDirectory())) {
			error(" not exists or is Dir ->  " + fileNameV0);
		}

		File file1 = new File(baseDir, fileNameV0);
		if ((!file1.exists()) || (file1.isDirectory())) {
			error(" not exists or is Dir ->  " + fileNameV1);
		}

		try {

			if (!getClient().existsBucket("test-put-version-simple")) {
				getClient().createBucket("test-put-version-simple");
			}
			bucket_1 = getClient().getBucket("test-put-version-simple");

		} catch (ODClientException e) {
			logger.error(e);
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		try {
			if (!getClient().isEmpty(bucket_1.getName())) {
				logger.debug("emptyting test bucket " + bucket_1.getName());
				ResultSet<Item<ObjectMetadata>> r = getClient().listObjects(bucket_1);
				while (r.hasNext()) {
					Item<ObjectMetadata> i = r.next();
					if (i.isOk()) {
						logger.debug("deleting ->  b:" + i.getObject().getBucketName() + " o:" + i.getObject().getObjectName());
						getClient().deleteObject(i.getObject().getBucketName(), i.getObject().getObjectName());
					}
				}
			}
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

		String bucketName = bucket_1.getName();

		File file0 = new File(baseDir, fileNameV0);

		String objectName = FSUtil.getBaseName(file0.getName()) + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 100000))).intValue());
		objectName = getClient().normalizeObjectName(objectName);

		try {

			// version 0
			
			getClient().putObject(bucketName, objectName, file0);
			logger.debug("putObject -> b: " + bucketName + " o:" + objectName);
			testFiles.put(bucketName + "-" + objectName, new TestFile(file0, bucketName, objectName));

		} catch (ODClientException e) {
			error(String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
		}

		try {
			// version 1
			
			File file1 = new File(baseDir, fileNameV1);

			getClient().putObject(bucketName, objectName, file1);
			logger.debug("putObject new version -> b:" + bucketName + " o:" + objectName);
			testFiles.get(bucketName + "-" + objectName).addSrcFileVersion(file1);

		} catch (ODClientException e) {
			error(String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
		}

		validateSet(testFiles, "validateSet");

		logger.info(mname + " total -> " + String.valueOf(getCounter()));
		getMap().put(mname, "ok " + String.valueOf(counter));

		return true;

	}

	/**
	 * if ( dateTimeDifference( showStatus, OffsetDateTime.now(),
	 * ChronoUnit.MILLIS)>THREE_SECONDS) { logger.info( mname + " -> " +
	 * String.valueOf(getCounter())); showStatus = OffsetDateTime.now(); }
	 * 
	 * @return
	 */

	protected int getCounter() {
		return counter;
	}

	/**
	 * private boolean putObjectNewVersion(String mname) {
	 * 
	 * logger.debug("Starting " + mname);
	 * 
	 * counter = 0;
	 * 
	 * testFiles.forEach((k, v) -> { String srcname = v.getSrcFile(0).getName();
	 * 
	 * String name = FilenameUtils.getBaseName(srcname); String ext =
	 * FilenameUtils.getExtension(srcname);
	 * 
	 * String nameNewVersion = getSourceV1Dir() + File.separator + name + "-v1" +
	 * "." + ext;
	 * 
	 * try { if ((new File(nameNewVersion)).exists()) FileUtils.forceDelete(new
	 * File(nameNewVersion));
	 * 
	 * } catch (IOException e) { error(e.getClass().getName() + " - can not delete
	 * existing new version locally"); }
	 * 
	 * try { Files.copy(v.getSrcFile(0).toPath(), new File(nameNewVersion).toPath(),
	 * StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) {
	 * error(e.getClass().getName() + " - can copy version locally"); }
	 * 
	 * try {
	 * 
	 * 
	 * 
	 * getClient().putObject(v.bucketName, v.objectName, new File(nameNewVersion));
	 * 
	 * counter++; testFiles.get(v.bucketName + "-" +
	 * v.objectName).addSrcFileVersion(new File(nameNewVersion));
	 * 
	 * sleep();
	 * 
	 * if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) >
	 * THREE_SECONDS) { logger.info(mname + " -> " + String.valueOf(getCounter()));
	 * showStatus = OffsetDateTime.now(); }
	 * 
	 * } catch (ODClientException e) {
	 * logger.error(String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " " +
	 * String.valueOf(e.getErrorCode())); error(e); }
	 * 
	 * });
	 * 
	 * logger.info(mname + " total -> " + String.valueOf(getCounter()));
	 * 
	 * boolean success = validateSet(testFiles);
	 * 
	 * if (success) logger.debug(mname + " ok"); else logger.debug(mname + "
	 * error");
	 * 
	 * getMap().put(mname, success ? "ok" : "error");
	 * 
	 * return success;
	 * 
	 * }
	 */

	private boolean validateSet(Map<String, TestFile> mv, String mname) {

		logger.debug(" Starting " + mname);

		mv.forEach((k, testFile) -> {

			// download head version -----------------------

			{
				ObjectMetadata meta = null;

				try {

					meta = getClient().getObjectMetadata(testFile.bucketName, testFile.objectName);

				} catch (ODClientException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}

				File destFile = new File(downloadDir, meta.fileName);

				try {
					if ((destFile).exists())
						FileUtils.forceDelete(destFile);

				} catch (IOException e) {
					error(e.getClass().getName() + " | FileUtils.forceDelete( " + destFile.getAbsolutePath() + ");");
				}
				try {
					getClient().getObject(meta.bucketName, meta.objectName, destFile.getAbsolutePath());

				} catch (ODClientException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
				logger.debug("download -> b: " + meta.bucketName + " o:" + meta.objectName + " f: " + destFile.getAbsolutePath());

				try {

					String src_sha = testFile.getSrcFileSha256(meta.version);
					String new_sha = OdilonFileUtils.calculateSHA256String(destFile);

					if (!src_sha.equals(new_sha)) {
						logger.error("sha256 are not equal -> b:" + meta.bucketName + " o:" + meta.objectName + " f_src: " + testFile.getSrcFile(meta.version).getAbsolutePath() + " dnload: " + destFile.getAbsolutePath());
						error("sha256 are not equal -> b:" + meta.bucketName + " o:" + meta.objectName + " f_src: " + testFile.getSrcFile(meta.version).getAbsolutePath() + " dnload: " + destFile.getAbsolutePath());
					}

				} catch (NoSuchAlgorithmException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}
			// download previous version -----------------------


			{
				logger.debug("download previous version ");

				
				ObjectMetadata prev = null;

				try {
					prev = getClient().getObjectMetadataPreviousVersion(testFile.bucketName, testFile.objectName);
					logger.debug(prev.toString());
					logger.debug();
					
				} catch (ODClientException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}

				File destPrevFile = new File(downloadDir, prev.fileName);

				InputStream is = null;
				
				try {
				
						is = getClient().getObjectPreviousVersion(testFile.bucketName, testFile.objectName);
						FileUtils.copyInputStreamToFile(is, destPrevFile);
				
					
				} catch (ODClientException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
				
				finally {
					if (is!=null) {
					
						try {
							is.close();
						} catch (IOException e) {
							error(e.getClass().getName() + " | " + e.getMessage());
						}
					}
				}
				
				logger.debug("downloaded ->  b:" + prev.bucketName + " o:" + prev.objectName + " f: " + destPrevFile.getAbsolutePath() + " size: " + destPrevFile.length() + " bytes");

				try {

					String src_sha = testFile.getSrcFileSha256(prev.version);
					String new_sha = OdilonFileUtils.calculateSHA256String(destPrevFile);

					if (!src_sha.equals(new_sha)) {
						logger.error("sha256 are not equal -> b:" + prev.bucketName + " o:" + prev.objectName + " f_src: " + testFile.getSrcFile(prev.version).getAbsolutePath() + " dnload: " + destPrevFile.getAbsolutePath());
						error("sha256 are not equal -> b:" + prev.bucketName + " o:" + prev.objectName + " f_src: " + testFile.getSrcFile(prev.version).getAbsolutePath() + " dnload: " + destPrevFile.getAbsolutePath());
					}

				} catch (NoSuchAlgorithmException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
				 
			}

			/**
			if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
				logger.info("validateSet -> " + String.valueOf(getCounter()));
				showStatus = OffsetDateTime.now();
			}
			**/
		});

		logger.debug(mname, "ok");
		getMap().put(mname, "ok");

		return true;
	}

}
