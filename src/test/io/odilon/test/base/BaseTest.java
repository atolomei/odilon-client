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
package io.odilon.test.base;


import org.junit.Test;

import io.odilon.client.ODClient;
import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.RedundancyLevel;
import io.odilon.util.OdilonFileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public abstract class BaseTest {

	private static final Logger logger = Logger.getLogger(BaseTest.class.getName());

	public static long THREE_SECONDS = 3000;
	
	public final String DOWNLOAD_STAND_BY_DIR  = "d:"+File.separator+ "test-files-standby-download";
	
	private String SRC_DIR = "d:"+File.separator+"test-files";
	private String DOWNLOAD_DIR = "d:"+File.separator+"test-files-download";
	
	public String SRC_DIR_V0 = SRC_DIR + File.separator + "v0";
	public String SRC_DIR_V1 = SRC_DIR + File.separator + "v1";
	public String SRC_DIR_V2 = SRC_DIR + File.separator + "v2";
	
	public String DOWNLOAD_DIR_V0 = DOWNLOAD_DIR + File.separator+"v0";
	public String DOWNLOAD_DIR_V1 = DOWNLOAD_DIR + File.separator+"v1";
	public String DOWNLOAD_DIR_V2 = DOWNLOAD_DIR + File.separator+"v2";
	
	public String DOWNLOAD_DIR_RESTORED = DOWNLOAD_DIR + File.separator+"restored";
	
	
	public String endpoint = "localhost";
	
	
	public int port = 9211;

	private String accessKey = "odilon";
	private String secretKey = "odilon";
	private OdilonClient client;
	private Bucket testBucket;

	private int max = 2000;
	
	private long max_length = 500 * 100 * 10000; // 500 MB
	
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private Map<String, String> map = new TreeMap<String, String>();
	
	private long LAPSE_BETWEEN_OP_MILLISECONDS = 0;

	private String standByEndpoint;
	private int standByPort;
	
	private String standByAccessKey = "odilon";
	private String standBySecretKey = "odilon";
	private boolean standBySSL = false;
	
	private ODClient standByClient;
	

	public boolean isSSL = false;
	public boolean isAcceptAllCertificates = true;
	
	
	
	public void close() {
		if (this.client!=null) {
			try {
				this.client.close();
			} catch (ODClientException e) {
				error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
			}
		}
	}
	/**
	 * <p>Used by stand alone test</p>
	 */
	public BaseTest() {
		this(null);
	}
	
	/**
	 * <p>Used by RegressionTest</p>
	 * 
	 * @param client
	 */
	public BaseTest(OdilonClient client) {
		
		// setSSL(true);
		
		logger.debug("Start " + this.getClass().getName());
		
		String tempDir = System.getProperty("tempDir");
		String downloadDir = System.getProperty("downloadDir");
				
		//String tempEndpoint = System.getProperty("endpoint");
		//String tempPort = System.getProperty("port");
	
		String lapse = System.getProperty("sleepMilliseconds");
		
		if (lapse!=null)
			LAPSE_BETWEEN_OP_MILLISECONDS  = Long.valueOf(lapse.trim());
		
		String max = System.getProperty("max");
		if (max!=null)
			setMax(Integer.valueOf(max.trim()));

		String maxLength = System.getProperty("maxLength");				
		if (maxLength!=null)
			max_length = Long.valueOf(maxLength.trim());

		
		if (tempDir!=null)
			SRC_DIR=tempDir.trim();
		
		if (downloadDir!=null)
			DOWNLOAD_DIR=downloadDir.trim();

		//if (tempEndpoint!=null)
		//	endpoint=tempEndpoint.trim();

		//if (tempPort!=null)
		//	port= Integer.valueOf(tempPort.trim());
		
		setClient(client);
		
	}

	
	public void setClient(OdilonClient client) {
		this.client = client;
	}

	
	public ODClient getStandByClient() {
		 
		if (!isStandBy()) {
			return null;
		}
		
		if (this.standByClient==null) {
		try {
			
				this.standByClient = new ODClient(getStandByEndpoint(), getStandByPort(), standByAccessKey, standBySecretKey, standBySSL);
				logger.debug(standByClient.toString());
		        
			} catch (Exception e) {
				error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
			}
		}
		
		 return standByClient;
	}
	
	
	public int getStandByPort() {
		
		if (getStandByEndpoint()!=null) {
			return this.standByPort;
		}
		return -1;
	}
	

	public String getStandByEndpoint() {

		if (standByEndpoint==null) {
	
			if (isStandBy()) {
				try {
					
					this.standByEndpoint = getClient().systemInfo().standbyUrl;
					this.standByPort = Integer.valueOf(getClient().systemInfo().standbyPort);
					
				} catch (ODClientException e) {
					error(e);
				}
			}
		}
		
		return standByEndpoint;
	}
	

	
		
	public void removeTestBucket() {
		try {
			
			if (testBucket==null)
				return;
			
			String bucketName = testBucket.getName();
			
			if (getClient().existsBucket(bucketName)) {
					getClient().deleteBucket(bucketName);
					logger.debug("removeTestBucket. " + bucketName +" -> ok");
					getMap().put("removeTestBucket. " + bucketName, "ok");
			}
		} catch (ODClientException e) {
				logger.error(e);
				error(e);
		}
	}
	
	
	
	public Bucket createTestBucket(String bucketName) {

		try {
			if (!getClient().existsBucket(bucketName)) {
					getClient().createBucket(bucketName);
					logger.debug("createTestBucket. " + bucketName + " -> ok");
					getMap().put("createTestBucket. " + bucketName, "ok");
			}
			return getClient().getBucket(bucketName);
			
		} catch (ODClientException e) {
			error(e);
			return null;
		}
	}

	
	public boolean testPing() {
		try {
			String p=ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				logger.debug("ping " +  getClient().getUrl() + " -> ok");
				map.put("ping  " +  getClient().getUrl() , "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		return true;
	}
	
	
	public boolean preCondition() {
		return testPing();
	}
	
	
	public void error(Exception e) {
		error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
	}
	
	public void error(String string) {
		logger.error(string);
		System.exit(1);
	}
	
	public Map<String, String> getMap() {
		return map;
	}
	
	public String ping() {
		return getClient().ping();
	}
	
	@Test
	public abstract void executeTest();
	
	public boolean isPdf(String filename) {
		return filename.toLowerCase().matches("^.*\\.(pdf)$"); 
	}
	
	public void showResults() {
		logger.debug("Results");
		logger.debug("-------");
		getMap().forEach((k,v) -> logger.debug(k+" -> "+ v));
	    logger.debug("done");
		
	}

	
	public long getSleepDurationMills() {
		return LAPSE_BETWEEN_OP_MILLISECONDS;
	}

	public void setSleepDurationMills(long lAPSE_BETWEEN_OP_MILLISECONDS) {
		LAPSE_BETWEEN_OP_MILLISECONDS = lAPSE_BETWEEN_OP_MILLISECONDS;
	}
	
	public void setSSL( boolean b) {
		this.isSSL=b;
	}
	
	public boolean isSSL() {
		return this.isSSL;
	}

	public boolean isAcceptAllCertificates() {
		return this.isAcceptAllCertificates;
	}
	
	public void setAcceptAllCertificates(boolean b) {
		this.isAcceptAllCertificates=b;
	}
	
	
	public OdilonClient getClient() {
		try {
			if (client==null) {
					this.client = new ODClient((isSSL()?"https":"http") + "://" + endpoint, port, accessKey, secretKey, isSSL(), isAcceptAllCertificates());
			        logger.debug(this.client.toString());
			}
	        
		} catch (Exception e) {
			error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
		}
		return client;
	}
	
	
	public String randomString(int size) {
		int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength =  size;
	    Random random = new Random();
	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();
	    return generatedString;
	}

	protected long dateTimeDifference(Temporal d1, Temporal d2, ChronoUnit unit) {
        return unit.between(d1, d2);
    }
	
	protected int getMax() {
		return max;
	}

	protected void setMax(Integer n) {
		this.max=n.intValue();
	}

	protected void setMaxLength(Long n) {
		this.max_length=n.longValue();
	}

	protected long getMaxLength() {return this.max_length;}
	
	protected boolean isElegible(File file) {
		
		if (file.isDirectory())
			return false;
		
		if (file.length()>max_length)
			return false;
		
		if (	FSUtil.isText(file.getName()) 		|| 
				FSUtil.isText(file.getName()) 		|| 
				FSUtil.isPdf(file.getName())  		|| 
				FSUtil.isImage(file.getName()) 		|| 
				FSUtil.isMSOffice(file.getName()) 	||
				FSUtil.isJar(file.getName()) 		||
				FSUtil.isAudio(file.getName()) 		||
				FSUtil.isVideo(file.getName()) 		||
				FSUtil.isExecutable(file.getName()) ||
				FSUtil.isZip(file.getName()))
			
			return true;
		
		return false;
	}

	
	/**
	 * @return
	 */
	protected boolean putObjects(String bucketName) {
		
        File dir = new File(SRC_DIR_V0);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
		}
        
		int counter = 0;
		
		
		for (File fi:dir.listFiles()) {
			
			if (counter == getMax())
				break;
			
			if (!fi.isDirectory() && (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName()) || FSUtil.isZip(fi.getName())) && (fi.length()<getMaxLength())) {
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
	
	
	/**
	 * 
	 * 
	 */
	protected boolean isVersionControl() {
		if (getClient()!=null) {
			try {
				return getClient().isVersionControl();
			} catch (ODClientException e) {
				error(e);
			}
		}
		return false;
	}

	
	/**
	 * 
	 * 
	 */
	protected boolean isRAIDSix() {
		if (getClient()!=null) {
			try {
				return (getClient().systemInfo().redundancyLevel==RedundancyLevel.RAID_6);
			} catch (ODClientException e) {
				error(e);
			}
		}
		return false;
	}


	/**
	 * 
	 * 
	 */
	protected boolean isRAIDZero() {
		if (getClient()!=null) {
			try {
				return (getClient().systemInfo().redundancyLevel==RedundancyLevel.RAID_0);
			} catch (ODClientException e) {
				error(e);
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * 
	 */
	protected boolean isStandBy() {
		
		if (getClient()!=null) {
		
			try {
			
				if  ((	getClient().systemInfo().isStandby!=null) &&
				    	getClient().systemInfo().isStandby.equals("true"))
					
					return true;
				
			} catch (ODClientException e) {
				error(e);
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * 
	 */
	protected void sleep() {
		
		if (LAPSE_BETWEEN_OP_MILLISECONDS>0) {
			try {
				Thread.sleep(LAPSE_BETWEEN_OP_MILLISECONDS);
			} catch (InterruptedException e) {
			}
		}
	}	
	
	
}
 