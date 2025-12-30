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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

/**
 * 
 * Put Object Get Object Get PresignedUrl Remove Object
 *
 *
 */
public class TestTunnelPutGet extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestTunnelPutGet.class.getName());

	private String sourceDir;
	private String downloadDir;

	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

	private OffsetDateTime showStatus = OffsetDateTime.now();
	private int sub_index = 0;

	final String base64Credentials = Base64.getEncoder().encodeToString("root:..resurrecto$".getBytes());

	public TestTunnelPutGet() {
	}

	List<String> urls = new ArrayList<String>();

	@Test
	public void executeTest() {

		preCondition();

		String s1 = "http://localhost:8087/webdav/aerolineas-btv/content/files/document/2022/06/13fa7b90-e1ac-11ec-abf9-0050569415cd/reporte%20y%20seguimiento%20da%F1os%20a%20aeronaves%20firmas%20(1).pdf";
		urls.add(s1);
 
		if (!testAddObjectsTunnel("tunnel"))
			error("tunnel");

		showResults();
	}

 
	public boolean testAddObjectsTunnel(String version) {

		Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

		int counter = 0;
		String bucketName = this.bucket_1.getName();

		for (String url : urls) {

			if (counter >= getMax())
				break;

			HttpURLConnection connection = null;
			URL httpurl;

			try {
				httpurl = new URL(url);

			} catch (MalformedURLException e) {
				error(e);
				return false;
			}

			try {
				httpurl = new URL(url);
				connection = (HttpURLConnection) httpurl.openConnection();
				connection.setRequestProperty("Authorization", "Basic " + base64Credentials);
				connection.setReadTimeout(60000);
				connection.setConnectTimeout(60000);
				connection.setRequestMethod("GET");
				connection.setRequestProperty("User-Agent", ODClient.DEFAULT_USER_AGENT);

			} catch (Exception e) {
				error(e);
				return false;
			}

			testConnection(connection);

			String fileName =   normalizeFileName( getFileName(url) );
			String objectName = FSUtil.getBaseName(fileName) + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue());

			long contentLength = connection.getContentLengthLong();
 
			try (InputStream inputStream = connection.getInputStream()) {

				List<String> customTags = new ArrayList<String>();
				customTags.add(String.valueOf(counter));

				ObjectMetadata meta = getClient().putObjectStream(
						bucketName, 
						objectName, inputStream, 
						Optional.of(fileName), 
						Optional.of(contentLength), 
						Optional.of(getClient().getContentType(fileName)),
						Optional.ofNullable(customTags));

				logger.info(String.valueOf(testFiles.size() + " testTunnel -> " + fileName + " | " + String.valueOf(meta.getLength() / 1000) + "KB"));
				counter++;

				sleep();

				if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
					logger.info("testTunnel add -> " + String.valueOf(testFiles.size()));
					showStatus = OffsetDateTime.now();
				}

			} catch (ODClientException e) {
				error("Http status " + String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
				return false;
			} catch (FileNotFoundException e1) {
				error(e1);
			} catch (IOException e) {
				error(e);
				return false;

			} catch (InternalCriticalException e) {
				error(e);
				return false;
			}
		}

		return true;

	}

	/**
	 * @param connection
	 */
	
	private void testConnection(HttpURLConnection connection) {

		try {
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				error("response code not ok -> " + String.valueOf(responseCode));
			} else {
				logger.debug("Connection -> OK");
			}

		} catch (Exception e) {
			error(e);
		}

	}
	
	public String normalizeFileName(String name) {
		String basename=FileNameUtils.getBaseName(name);
		String extension = FileNameUtils.getExtension(name);

		String str = basename.replaceAll("[^\\x00-\\x7F]|[\\s]+", "-").toLowerCase().trim();
		str = str.replace(",", "");
		str=str.replace("%20", "-");
		str=str.replace("(", "-");
		str=str.replace(")", "-");
		str=str.replace("%f1", "");
		str=str.replace("--", "-");
		
		while (str.endsWith("-") && str.length()>1) {
			str=str.substring(0, str.length()-2);
		}
		return str+"."+extension;
	
	}

	private String getFileName(String url) {
		if (url == null)
			return null;
		String arr[] = url.split("/");
		return arr[arr.length - 1];
	}

	/**
	 * 
	 * 
	 */
	public boolean preCondition() {

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

		downloadDir = getDownloadDirHeadVersion();
		File dndir = new File(downloadDir);

		sourceDir = super.getSourceDir();
		File dir = new File(sourceDir);

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
	// if (!testAddObjectsNoTunnel("no tunnel")) {
	// error("no tunnel");
	// }
/**
public boolean testAddObjectsNoTunnel(String version) {

int counter = 0;
String bucketName = this.bucket_1.getName();

{

	Path targetPath = Paths.get("/Users/alejandrotolomei/Downloads/", "tolomei.jpg");

	String fileName = getFileName(targetPath.toFile().getName());
	String objectName = FSUtil.getBaseName(fileName) + "-" + String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue());
	long contentLength = targetPath.toFile().length();

	try (InputStream inputStream = new BufferedInputStream(new FileInputStream(targetPath.toFile()))) {

		List<String> customTags = new ArrayList<String>();
		customTags.add(String.valueOf(counter));

		ObjectMetadata meta = getClient().putObjectStream(bucketName, objectName, inputStream, Optional.of(fileName), Optional.of(contentLength), Optional.of(getClient().getContentType(fileName)), Optional.ofNullable(customTags));

		testFiles.put(bucketName + "-" + objectName, new TestFile(targetPath.toFile(), bucketName, objectName));

		logger.info(String.valueOf(testFiles.size() + " | test no Tunnel -> " + fileName + " | " + String.valueOf(meta.getLength() / 1000) + " KB"));
		counter++;

		sleep();

		if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
			logger.info("test no Tunnel add -> " + String.valueOf(testFiles.size()));
			showStatus = OffsetDateTime.now();
		}

	} catch (ODClientException e) {
		error("Http status " + String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " | Odilon ErrCode: " + String.valueOf(e.getErrorCode()));
	} catch (FileNotFoundException e1) {
		error(e1);
	} catch (IOException e) {
		error(e);
		return false;
	}
}

logger.info("test no Tunnel add total -> " + String.valueOf(testFiles.size()));

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
			str.append("test no Tunnel Error sha256 are not equal -> " + meta.bucketName + " / " + meta.objectName);
			str.append(" | src -> " + v.getSrcFile(0).getAbsolutePath() + "  " + String.valueOf(v.getSrcFile(0).length() / 1000.0) + " kbytes");
			str.append(" | dest -> " + (new File(destFileName)).getAbsolutePath() + "  " + String.valueOf(new File(destFileName).length() / 1000.0) + " kbytes");
			error(str.toString());
		}

	} catch (NoSuchAlgorithmException | IOException e) {
		logger.error(e);
		error(e);
	}

	if (dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS) > THREE_SECONDS) {
		logger.info("testTunnel check -> " + String.valueOf(sub_index) + " | " + meta.getFileName());
		showStatus = OffsetDateTime.now();
	}
});

logger.info("testTunnel -> ok " + String.valueOf(testFiles.size()));

getMap().put("testTunnel " + version + " | " + String.valueOf(testFiles.size()), "ok");

return true;

}

*/

	// Path targetPath = Paths.get("/Users/alejandrotolomei/Downloads",
	// "reporte.pdf");
	// String fileName = getFileName( targetPath.toFile().getName());
	// String objectName = FSUtil.getBaseName(fileName) + "-" +
	// String.valueOf(Double.valueOf((Math.abs(Math.random() * 10000))).intValue());



	// try (InputStream inputStream = new FileInputStream(targetPath.toFile())) {
	// try (InputStream inputStream = new
	// BufferedInputStream(connection.getInputStream())) {

	
}
