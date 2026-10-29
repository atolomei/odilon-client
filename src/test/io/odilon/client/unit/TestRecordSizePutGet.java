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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

/**
 * <p>
 * Tests the server's v2 record-based encryption at the record-size
 * boundaries: {@code io.odilon.service.ServerSettings#encryptionRecordSize}
 * (property {@code encryption.recordSize}).
 * </p>
 * <p>
 * Generates 5 files with random binary data in the directory configured by
 * {@code encryption.recordSize.source.dir}:
 * </p>
 * <ul>
 * <li>smaller than {@code encryption.recordSize} (0.5x)</li>
 * <li>exactly equal to {@code encryption.recordSize} (1x)</li>
 * <li>1.5x the record size</li>
 * <li>exactly 2x the record size</li>
 * <li>2.5x the record size</li>
 * </ul>
 * <p>
 * Each file is uploaded and then downloaded (to {@code download.dir}), and
 * the SHA-256 of the downloaded file is compared to the source, same as
 * {@link TestObjectPutGet}.
 * </p>
 * <p>
 * If the server does not have encryption enabled
 * ({@code SystemInfo.isEncryptEnabled != "true"}) the test does nothing and
 * returns.
 * </p>
 */
public class TestRecordSizePutGet extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestRecordSizePutGet.class.getName());

	private static final String BUCKET_NAME = "dev-test-recordsize";

	private String sourceDir;
	private String downloadDir;

	private Bucket bucket = null;
	private Map<String, TestFile> testFiles = new LinkedHashMap<String, TestFile>();

	private OffsetDateTime showStatus = OffsetDateTime.now();
	private int sub_index = 0;

	public TestRecordSizePutGet() {
	}

	@Test
	@Override
	public void executeTest() {

		try {
			String p = ping();
			if (p == null || !p.equals("ok")) {
				error("ping  -> " + (p != null ? p : "null"));
			} else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}

		if (!isEncryptionEnabled()) {
			logger.info("Server does not have encryption enabled, skipping testRecordSizePutGet");
			getMap().put("testRecordSizePutGet", "skipped (encryption not enabled on server)");
			showResults();
			return;
		}

		preCondition();

		if (!testRecordSizeBoundaries())
			error("testRecordSizeBoundaries");

		showResults();
	}

	/**
	 * <p>
	 * Generates the 5 boundary-size files, uploads them, downloads them and
	 * compares SHA-256 with the source, same as
	 * {@link TestObjectPutGet#testAddObjectsStream}.
	 * </p>
	 */
	public boolean testRecordSizeBoundaries() {

		final long recordSize = getEncryptionRecordSize();

		/** label -> size in bytes */
		Map<String, Long> sizes = new LinkedHashMap<String, Long>();
		sizes.put("0.5x-smaller", recordSize / 2);
		sizes.put("1x-equal", recordSize);
		sizes.put("1.5x-larger", recordSize + (recordSize / 2));
		sizes.put("2x-larger", recordSize * 2);
		sizes.put("2.5x-larger", (recordSize * 2) + (recordSize / 2));

		logger.info("encryption.recordSize = " + String.valueOf(recordSize) + " bytes | generating " + String.valueOf(sizes.size()) + " files in -> " + sourceDir);

		String bucketName = this.bucket.getName();

		testFiles.clear();

		for (Map.Entry<String, Long> entry : sizes.entrySet()) {

			String label = entry.getKey();
			long size = entry.getValue().longValue();

			File file = new File(sourceDir + File.separator + "recordsize-" + label + "-" + String.valueOf(size) + "bytes.bin");

			try {
				generateRandomFile(file, size);
			} catch (IOException e) {
				error(e);
			}

			logger.info("generated -> " + file.getName() + " | size: " + String.valueOf(file.length()) + " bytes");

			String objectName = getClient().normalizeObjectName("recordsize-" + label + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue()));

			try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {

				List<String> customTags = new ArrayList<String>();
				customTags.add(label);

				getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.of(Long.valueOf(file.length())), Optional.empty(), Optional.ofNullable(customTags));

				testFiles.put(bucketName + "-" + objectName, new TestFile(file, bucketName, objectName));

				sleep();

			} catch (ODClientException e) {
				error("Http status " + String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
			} catch (FileNotFoundException e1) {
				error(e1);
			} catch (IOException e2) {
				error(e2);
			}
		}

		logger.info("testRecordSizeBoundaries upload total -> " + String.valueOf(testFiles.size()));

		sub_index = 0;

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
					str.append("testRecordSizeBoundaries Error sha256 are not equal -> " + meta.bucketName + " / " + meta.objectName);
					str.append(" | src -> " + v.getSrcFile(0).getAbsolutePath() + "  " + String.valueOf(v.getSrcFile(0).length()) + " bytes");
					str.append(" | dest -> " + (new File(destFileName)).getAbsolutePath() + "  " + String.valueOf(new File(destFileName).length()) + " bytes");
					error(str.toString());
				}

			} catch (NoSuchAlgorithmException | IOException e) {
				logger.error(e);
				error(e);
			}

			if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
				logger.info("testRecordSizeBoundaries checking -> " + String.valueOf(sub_index));
				showStatus = OffsetDateTime.now();
			}
		});

		logger.info("testRecordSizeBoundaries -> ok " + String.valueOf(testFiles.size()));

		getMap().put("testRecordSizeBoundaries | recordSize=" + String.valueOf(recordSize) + " | " + String.valueOf(testFiles.size()) + " files", "ok");

		return true;
	}

	/**
	 * <p>
	 * Writes {@code sizeBytes} of random data to {@code file}, streaming in
	 * chunks so files several times the record size (potentially tens/hundreds
	 * of MB) do not require a single huge in-memory buffer.
	 * </p>
	 */
	private void generateRandomFile(File file, long sizeBytes) throws IOException {

		final int CHUNK = 64 * 1024;

		SecureRandom random = new SecureRandom();
		byte[] buffer = new byte[CHUNK];

		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
			long remaining = sizeBytes;
			while (remaining > 0) {
				int toWrite = (int) Math.min(CHUNK, remaining);
				random.nextBytes(buffer);
				os.write(buffer, 0, toWrite);
				remaining -= toWrite;
			}
		}
	}

	/**
	 * <p>
	 * Ensures the record-size source dir, a dedicated download dir and a
	 * dedicated test bucket exist. Called only when the server has encryption
	 * enabled.
	 * </p>
	 */
	public boolean preCondition() {

		sourceDir = super.getEncryptionRecordSizeSourceDir();
		downloadDir = super.getDownloadDir() + File.separator + "recordsize";

		{
			File dir = new File(sourceDir);
			if ((!dir.exists()) || (!dir.isDirectory())) {
				try {
					FileUtils.forceMkdir(dir);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}
		}

		{
			File dir = new File(downloadDir);

			if ((dir.exists()) && (dir.isDirectory())) {
				try {
					FileUtils.forceDelete(dir);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}
			try {
				FileUtils.forceMkdir(dir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}

		try {
			if (!getClient().existsBucket(BUCKET_NAME)) {
				getClient().createBucket(BUCKET_NAME);
				logger.debug("Bucket " + BUCKET_NAME + " created");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

			this.bucket = getClient().getBucket(BUCKET_NAME);

			return true;

		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
			return false;
		}
	}

}
