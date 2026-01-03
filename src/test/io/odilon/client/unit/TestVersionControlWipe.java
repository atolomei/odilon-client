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
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import java.util.Map;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

public class TestVersionControlWipe extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());

	private Bucket bucket;

	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

	public TestVersionControlWipe() {
	}

	@Override
	public void executeTest() {

		if (!preCondition())
			error("preCondition");

		if (!wipeBucket())
			error("wipeBucket");

		showResults();
	}

	private boolean wipeBucket() {

		try {

			getClient().deleteAllBucketVersions(bucket.getName());

			logger.debug("wipeBucket -> ok");
			getMap().put("wipeBucket", "ok");

		} catch (ODClientException e) {
			logger.error(e);
		}

		return true;
	}

	/**
	 * 
	 * 
	 */
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

		try {
			if (!getClient().isVersionControl())
				error("version control must be enabled");

		} catch (ODClientException e) {
			error(e);
		}

		try {

			if (!getClient().existsBucket("test-put-version")) {
				getClient().createBucket("test-put-version");
			}

			bucket = getClient().getBucket("test-put-version");

			if (getClient().isEmpty(bucket.getName())) {
				testAddObjects();
			}

		} catch (ODClientException e) {
			error(e);
		}

		return true;
	}

	/**
	 * @return
	 */
	public boolean testAddObjects() {

		File dir = new File(getSourceDir());

		if ((!dir.exists()) || (!dir.isDirectory())) {
			throw new RuntimeException("Dir not exists or the File is not Dir -> " + getSourceDir());
		}

		int counter = 0;

		String bucketName = null;

		bucketName = this.bucket.getName();

		for (File fi : dir.listFiles()) {

			if (counter == getMaxFilesToTest())
				break;

			if (!fi.isDirectory()
					&& (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName()) || FSUtil.isZip(fi.getName()))
					&& (fi.length() < getMaxLength())) {
				String objectName = FSUtil.getBaseName(fi.getName()) + "-"
						+ String.valueOf(Double.valueOf((Math.abs(Math.random() * 100000))).intValue());
				
				objectName = getClient().normalizeObjectName(objectName);

				try {

					getClient().putObject(bucketName, objectName, fi);
					testFiles.put(bucketName + "-" + objectName, new TestFile(fi, bucketName, objectName));
					counter++;

				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " "
							+ String.valueOf(e.getErrorCode()));

				}
			}
		}

		logger.info("testAddObjects -> Total:  " + String.valueOf(testFiles.size()));

		testFiles.forEach((k, v) -> {
			ObjectMetadata meta = null;

			try {
				meta = getClient().getObjectMetadata(v.bucketName, v.objectName);

			} catch (ODClientException e) {
				error(e);
			}

			String destFileName = super.getDownloadDirHeadVersion() + File.separator + meta.fileName;

			try {
				getClient().getObject(meta.bucketName, meta.objectName, destFileName);

			} catch (ODClientException | IOException e) {
				error(e);
			}

			TestFile t_file = testFiles.get(meta.bucketName + "-" + meta.objectName);

			if (t_file != null) {

				try {
					String src_sha = t_file.getSrcFileSha256(0);
					String new_sha = OdilonFileUtils.calculateSHA256String(new File(destFileName));

					if (!src_sha.equals(new_sha)) {
						error("Error sha256 are not equal -> " + meta.bucketName + "-" + meta.objectName);
					}

				} catch (NoSuchAlgorithmException | IOException e) {
					error(e);
				}
			} else {
				error("Test file does not exist -> " + meta.bucketName + "-" + meta.objectName);
			}

		});

		logger.debug("testAddObjects", "ok");
		getMap().put("testAddObjects", "ok");
		return true;
	}

}
