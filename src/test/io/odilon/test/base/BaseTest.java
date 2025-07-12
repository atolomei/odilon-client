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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

/**
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public abstract class BaseTest {

	private static final Logger logger = Logger.getLogger(BaseTest.class.getName());

	public static long THREE_SECONDS = 3000;

	
	private String source_dir;
    private String source_dir_v1;
    private String source_dir_v2;

	private String download_dir;

	public String DOWNLOAD_DIR_HEAD;
	public String DOWNLOAD_DIR_V1;
	public String DOWNLOAD_DIR_V2;
	
	public String DOWNLOAD_DIR_RESTORED;
	public String DOWNLOAD_STAND_BY_DIR;

	public boolean isSSL = false;
	public boolean isAcceptAllCertificates = true;

	private OdilonClient client;
	private Bucket testBucket;

    private String serverHost = "localhost";
    private int port = 9234;
    
    private String accessKey = "odilon";
    private String secretKey = "odilon";
	
    private String presignedHost = serverHost;
	private int  presignedPort = 9234;
	private boolean presignedSSL = false;
	
	private int max = 10;
	
	private long max_length = 500 * 100 * 10000; // 500 MB
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	private Map<String, String> map = new TreeMap<String, String>();

	private long LAPSE_BETWEEN_OP_MILLISECONDS = 0;

	private String standByHost;
	private int standByPort;
	
	private String standByAccessKey = "odilon";
	private String standBySecretKey = "odilon";
	private boolean standBySSL = false;
	
	private ODClient standByClient;

	private Properties properties = new Properties();
	
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
		
		
		readConfigFiles();
		

         setMax((properties.get("max")!=null) ? Integer.valueOf(properties.get("max").toString().trim()) : 10);

		this.serverHost  =  (properties.get("odilon.server.host")!=null)  ? properties.get("odilon.server.host").toString().trim() : "localhost"; 
	    this.port        =  (properties.get("odilon.server.port")!=null)      ? Integer.valueOf(( properties.get("odilon.server.port").toString().trim())) : 80;
	    this.isSSL       =  (properties.get("odilon.server.isSSL")!=null)      ? Boolean.valueOf(( properties.get("odilon.server.isSSL").toString().trim())) : false;

	    this.accessKey   =  (properties.get("odilon.server.accessKey")!=null)  ? properties.get("odilon.server.accessKey").toString().trim() : "odilon";
	    this.secretKey   =  (properties.get("odilon.server.secretKey")!=null)  ? properties.get("odilon.server.secretKey").toString().trim() : "odilon";
	    
	    this.presignedHost =  (properties.get("odilon.server.presignedHost")!=null)     ? properties.get("odilon.server.presignedHost").toString().trim() : this.serverHost; 
	    this.presignedPort = (properties.get("odilon.server.presignedPort")!=null)      ? Integer.valueOf(( properties.get("odilon.server.presignedPort").toString().trim())) : this.port;
	    this.presignedSSL  = (properties.get("odilon.server.isPresignedSSL")!=null)      ? 
	            Boolean.valueOf(( properties.get("odilon.server.isPresignedSSL").toString().trim())) : this.isSSL;
	    
		this.source_dir    =  (properties.get("source.dir")!=null)    ? properties.get("source.dir").toString().trim()   : "./source"; 
		this.download_dir  =  (properties.get("download.dir")!=null)  ? properties.get("download.dir").toString().trim() : "./download"; 
		
		source_dir_v1 = source_dir + File.separator + "v1";
	    File dir_v1 = new File(source_dir_v1);
	        
	        if ( (!dir_v1.exists()) || (!dir_v1.isDirectory())) { 
				try {
						FileUtils.forceMkdir(dir_v1);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}

			source_dir_v2 = source_dir + File.separator + "v2";
		   File dir_v2 = new File(source_dir_v2);
		    if ( (!dir_v2.exists()) || (!dir_v2.isDirectory())) { 
				try {
						FileUtils.forceMkdir(dir_v2);
				} catch (IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}

		
		DOWNLOAD_DIR_HEAD = download_dir + File.separator+"head";
		DOWNLOAD_DIR_V1   = download_dir + File.separator+"v1";
		DOWNLOAD_DIR_V2   = download_dir + File.separator+"v2";
		
		DOWNLOAD_DIR_RESTORED = download_dir + File.separator+"restored";
		DOWNLOAD_STAND_BY_DIR  = "d:"+File.separator+ "test-files-standby-download";
	    
		//String tempDir = System.getProperty("tempDir");
		//String downloadDir = System.getProperty("downloadDir");
		//String lapse = System.getProperty("sleepMilliseconds");
		//if (lapse!=null)
		//	LAPSE_BETWEEN_OP_MILLISECONDS  = Long.valueOf(lapse.trim());
		//String maxLength = System.getProperty("maxLength");				
		//if (maxLength!=null)
	//		max_length = Long.valueOf(maxLength.trim());
		//if (tempDir!=null)
		//	source_dir=tempDir.trim();
		//if (downloadDir!=null)
		//	DOWNLOAD_DIR=downloadDir.trim();

		setClient(client);
		
	}

    public OdilonClient getClient() {
        try {
            if (client==null) {
                    this.client = new ODClient((isSSL()?"https":"http") + "://" + serverHost, port, accessKey, secretKey, isSSL(), isAcceptAllCertificates());
                    if (this.presignedHost!=null)
                        this.client.setPresignedUrl(this.presignedHost, this.presignedPort, this.presignedSSL);

                    logger.debug(this.client.toString());
                    logger.debug("");
            }
            
        } catch (Exception e) {
            error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
        }
        return client;
    }


    /**
     * 
     */
    public void close() {
        if (this.client!=null) {
            try {
                this.client.close();
            } catch (ODClientException e) {
                error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
            }
        }
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
			
				this.standByClient = new ODClient(getStandByHost(), getStandByPort(), standByAccessKey, standBySecretKey, standBySSL);
				logger.debug(standByClient.toString());
		        
			} catch (Exception e) {
				error(e.getClass().getName() +( e.getMessage()!=null ? (" | " + e.getMessage()) : ""));
			}
		}
		
		 return standByClient;
	}
	
	
	public int getStandByPort() {
		
		if (getStandByHost()!=null) {
			return this.standByPort;
		}
		return -1;
	}
	

	public String getStandByHost() {

		if (standByHost==null) {
	
			if (isStandBy()) {
				try {
					this.standByHost = getClient().systemInfo().standbyUrl;
					this.standByPort = Integer.valueOf(getClient().systemInfo().standbyPort);
				} catch (ODClientException e) {
					error(e);
				}
			}
		}
		return standByHost;
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
				logger.debug("ping " +  getClient().getSchemaAndHost() + " -> ok");
				map.put("ping  " +  getClient().getSchemaAndHost() , "ok");
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
	
	public void setSSL(boolean b) {
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
		
        File dir = new File(this.source_dir);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
        	error("Dir not exists or the File is not Dir -> " + source_dir);
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
			
		String destFileName = this.getDownloadDirHeadVersion() + File.separator + meta.fileName;
		
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
    
	 */
	protected void sleep() {
		
		if (LAPSE_BETWEEN_OP_MILLISECONDS>0) {
			try {
				Thread.sleep(LAPSE_BETWEEN_OP_MILLISECONDS);
			} catch (InterruptedException e) {
			}
		}
	}	

	   private void readConfigFiles() {
	        
	       String configPath  = "."+File.separator+"config" + File.separator+"application.properties";
	        
	       File file = new File(configPath);
	       
	        if (!file.exists()) {
	         
	            logger.error("config file does not exist -> " + configPath);
	            return;
	        }
	        
	        try (InputStream input = new FileInputStream(configPath)) {
	            properties.load(input);
	        } catch (IOException ex) {
	            throw new RuntimeException("Error ->  " + configPath);
	        }
	    }

	   public String getDownloadDirHeadVersion() {
		return DOWNLOAD_DIR_HEAD;
	   }

	   public String getSourceDir() {
		return this.source_dir;
	   }

	   public String getSourceV1Dir() {
		 		return source_dir_v1;
	   }

	   public String getSourceV2Dir() {
	 		return source_dir_v2;
  }
	
}
 
 

