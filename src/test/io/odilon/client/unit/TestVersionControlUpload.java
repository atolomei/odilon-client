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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.NumberFormatter;
import io.odilon.test.base.TestFile;
import io.odilon.util.ODFileUtils;

public class TestVersionControlUpload extends BaseTest {
				
	
	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	private static Random random = new Random();
	
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();

	private Bucket bucket_1;
	
	private AtomicInteger counterPutObject = new AtomicInteger(0);
	private AtomicInteger counterNewVersion =new AtomicInteger(0);
	private AtomicInteger counterDownloadDataVersion = new AtomicInteger(0);
	private AtomicInteger counterRestoreObjectVersion = new AtomicInteger(0);
	private AtomicInteger counterObjectMetadataPreviousVersion = new AtomicInteger(0);
				
	
	private OffsetDateTime showStatus = OffsetDateTime.now();
	
	private Map<Integer, File> secondVersion = new HashMap<Integer, File>();
	
	
	
	public TestVersionControlUpload() {
	
	}
	
	
	
	
	@Override
	public void executeTest() {
		
		if (!preCondition()) 
			error("preCondition");

		if (!putObject())
			error("putObject");
		
		if (!putObjectNewVersion())
			error("putObjectNewVersion");
		
		
		try {
			if (getClient().isVersionControl()) {
				
				if (!getObjectMetadataPreviousVersion())
					error("getObjectMetadataPreviousVersion");
				
				if (!downloadObjectDataVersions())
					error("downloadObjectDataVersions");
				
				if (!restoreObjectVersion())
					error("restoreObjectVersion");		
				
				
				
			}
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		
		showResults();
	}


	/**
	 * 
	 * 
	 */
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

		
		{
	        File dir = new File(SRC_DIR_V0);
	        
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
			}
		}

		{
	        File dir = new File(SRC_DIR_V1);
	    
	        FileUtils.deleteQuietly(dir);
	       	try {
				FileUtils.forceMkdir(dir);
			} catch (IOException e) {
				error(e);
			}
	        if ( (!dir.exists()) || (!dir.isDirectory())) { 
				error("Dir not exists or the File is not Dir -> " +SRC_DIR_V1);
			}
		}

		
		{
        File dir = new File(DOWNLOAD_DIR_V0);
        
        if ( (dir.exists()) && (dir.isDirectory())) { 
        	try {
				FileUtils.forceDelete(dir);
			} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(dir);
				
		} catch (IOException e) {
				logger.error(e);
				error(e.getClass().getName() + " | " + e.getMessage());
		}
		}
		

		{
	        File dir = new File(DOWNLOAD_DIR_V1);
	        
	        if ( (dir.exists()) && (dir.isDirectory())) { 
	        	try {
					FileUtils.forceDelete(dir);
				} catch (IOException e) {
					logger.error(e);
					error(e.getClass().getName() + " | " + e.getMessage());
				}
			}
	       	try {
					FileUtils.forceMkdir(dir);
					
			} catch (IOException e) {
					logger.error(e);
					error(e.getClass().getName() + " | " + e.getMessage());
			}
			}

        
		try {	
			
			if (!getClient().existsBucket("test-version-control")) {
				getClient().createBucket("test-version-control");
			}
			
			bucket_1 = getClient().getBucket("test-version-control");
			
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
	 * 
	 */
	private boolean putObject() {

		File dir = new File(SRC_DIR_V0);
		
		counterPutObject.set(0);	
		
		secondVersion.clear();
		
		
		String bucketName = bucket_1.getName();
		
		/** put files  **/
		
		for (File fi:dir.listFiles()) {

			if (counterPutObject.get() == getMax())
				break;
			
			if ( isValid(fi)) {
				
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				
				try {
					
					ObjectMetadata meta=getClient().putObject(bucketName, objectName, fi);
					TestFile test=new TestFile(fi, bucketName, objectName);
					test.addMetaVersion(meta);

					testFiles.put(bucketName+"-"+objectName, test);
					secondVersion.put(Integer.valueOf(counterPutObject.get()), fi);	
					
					counterPutObject.incrementAndGet();
					
					sleep();
					
					/** display status every 3 seconds or so */
					if ( dateTimeDifference(showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.debug( "putObject -> " + String.valueOf(counterPutObject.get()));
						showStatus = OffsetDateTime.now();
					}
					
				} catch (ODClientException e) {
					logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
					System.exit(1);
				}
			}
		}
		
		logger.debug( "putObject total -> (" + String.valueOf(counterPutObject.get()+")"));

		
		logger.debug( "putObject -> ok");
		getMap().put("putObject", "ok ("  + String.valueOf(counterPutObject)+")");
		
		return true;
	}
	
	private boolean isValid(File fi) {
		return ( (!fi.isDirectory()) && (FSUtil.isPdf(fi.getName())&& (fi.length()<getMaxLength())));
	}

	/**
	 * @return
	 */
	private boolean restoreObjectVersion() {

		
		File restoredDir = new File(DOWNLOAD_DIR_RESTORED);
		
		FileUtils.deleteQuietly(restoredDir);
		try {
			FileUtils.forceMkdir(restoredDir);
		} catch (IOException e) {
			error(e);
		}
		

		counterRestoreObjectVersion.set(0);
		
		testFiles.forEach((k,v) -> 
		{
			try {
				if (getClient().hasVersions(v.bucketName, v.objectName)) {
					
					ObjectMetadata headMeta = getClient().getObjectMetadata(v.bucketName, v.objectName);
					
					logger.debug("Metadata current -> " + headMeta.fileName + "  | version: " + String.valueOf(headMeta.version)  +" | " + NumberFormatter.formatFileSize(headMeta.length));
					
					
					getClient().restoreObjectPreviousVersions(v.bucketName, v.objectName);
					
					ObjectMetadata meta = getClient().getObjectMetadata(v.bucketName, v.objectName);
					
					logger.debug("Metadata restored -> " + meta.fileName +  "  | version: " + String.valueOf(meta.version) + " | "  + NumberFormatter.formatFileSize(meta.length));
					
					if (meta.version>=headMeta.version)
						error("meta version error for -> " + v.bucketName + " " + v.objectName);
					

					if ((new File(restoredDir, meta.fileName)).exists())
						FileUtils.deleteQuietly(new File(restoredDir, meta.fileName));
						
					
					InputStream isRestored = null;
					
					try {
						
						isRestored = getClient().getObject(v.bucketName, v.objectName);
						Files.copy(isRestored, (new File(restoredDir, meta.fileName)).toPath(), StandardCopyOption.REPLACE_EXISTING);
						logger.debug("Download restored -> " + meta.fileName +  "  | version: " + String.valueOf(meta.version) + " | " + NumberFormatter.formatFileSize(new File(restoredDir, meta.fileName).length()));
						
						counterRestoreObjectVersion.incrementAndGet();
						
						// validate
						
						try {
							
							String src_sha  =  v.getSrcFileSha256(0);
							String dest_sha =  ODFileUtils.calculateSHA256String(new File(restoredDir, meta.fileName));
							
							if (!dest_sha.equals(src_sha)) {
								error("Error sha256 are not equal -> " + 
										"b:" + meta.bucketName + "  o:" +
										meta.objectName + 
										" | downladed -> "  + meta.fileName  + " - " + NumberFormatter.formatFileSize((new File(restoredDir, meta.fileName)).length()) + " - " + 
										" | source -> " + v.getSrcFile(0).getName() + " -"  + NumberFormatter.formatFileSize(v.getSrcFile(0).length()));
							}
							else {
								logger.debug("restore ok ->"  + meta.fileName);
							}

							
							
							
							
						} catch (NoSuchAlgorithmException | IOException e) {
							error(e);
						}
						
					} catch (ODClientException e) {
						error(e.getClass().getName() + " | " + e.getMessage());
					}
					finally {
						
						if(isRestored!=null) {
							isRestored.close();
						}
					}
					
					sleep();
					
					/** display status every 3 seconds or so */
					
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.debug( "restoreObjectVersion -> " + String.valueOf(counterRestoreObjectVersion.get()));
						showStatus = OffsetDateTime.now();
					}
				}
			
			} catch (IOException e) {
				logger.error(e);
				error(e);
				
			} catch (ODClientException e) {
				logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				error(e);
			}
		});
		
		logger.debug( "restoreObjectVersion total -> (" + String.valueOf(counterRestoreObjectVersion.get()+")"));
		logger.debug("restoreObjectVersion ok");
		getMap().put("restoreObjectVersion", "ok");
		
		return true;
	}

	
	/**
	 * @return
	 */
	private boolean putObjectNewVersion() {
		
		logger.debug( "starting putObjectNewVersion");
		
		
	 	counterNewVersion.set(0);
	 	
		testFiles.forEach((k,v) -> 
		{
			String srcname=v.getSrcFile(0).getName();

			String name=FilenameUtils.getBaseName(srcname);
			String ext=FilenameUtils.getExtension(srcname);
			
			String nameSrcFileNewVersion=SRC_DIR_V1 + File.separator + name +"-v1" + "."+ext;
			
			// copy src v1 to src dir
			try {
				if ((new File(nameSrcFileNewVersion)).exists())
					FileUtils.forceDelete(new File(nameSrcFileNewVersion));
					
			} catch (IOException e) {
				error(e.getClass().getName()+ " - can not delete existing new version locally");
			}
			
			
			
			int index = random.nextInt(secondVersion.size());
					
			try {
				Files.copy(secondVersion.get(index).toPath(), 
							new File(nameSrcFileNewVersion).toPath(), 
							StandardCopyOption.REPLACE_EXISTING );
			} catch (IOException e) {
				error(e.getClass().getName() + " - can copy version locally");
			}
			
			try {
				
				/**  upload new version */  
				ObjectMetadata metadataV1 = getClient().putObject(v.bucketName, v.objectName, new File(nameSrcFileNewVersion));
				counterNewVersion.incrementAndGet();
				
				testFiles.get(v.bucketName+"-"+v.objectName).addSrcFileVersion(new File(nameSrcFileNewVersion));
				testFiles.get(v.bucketName+"-"+v.objectName).addMetaVersion(metadataV1);
				
				if (!getClient().hasVersions(v.bucketName, v.objectName))
					error("should have versions -> b:" + v.bucketName + " o:" + v.objectName);
				
				sleep();

				if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
					logger.debug( "putObjectNewVersion -> " + String.valueOf(counterNewVersion.get()));
					showStatus = OffsetDateTime.now();
				}
			} catch (IOException e) {
				logger.error(e);
				error(e);
				
			} catch (ODClientException e) {
				logger.error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				error(e);
			}
			
		}); 
		
		logger.debug( "putObjectNewVersion total -> (" + String.valueOf(counterNewVersion.get()+")"));
		
		boolean success = validateSetUploadNewVersion(testFiles);
		
		if (success) {
			logger.debug("putObjectNewVersion ok");
			getMap().put("putObjectNewVersion", "ok");
		}
		else {
			logger.debug("putObjectNewVersion error");
			getMap().put("putObjectNewVersion", success?"ok":"error");
		}
		
		return success;
	}
	

	/**
	 * 
	 * 
	 */
	private boolean downloadObjectDataVersions() {
		
		counterDownloadDataVersion.set(0);
		
		testFiles.forEach((k,test) -> 
		{
				try {
					
					if ( getClient().hasVersions(test.bucketName, test.objectName)) {
										
										
						ObjectMetadata metaCurrenVersion = getClient().getObjectMetadata(test.bucketName, test.objectName);
						ObjectMetadata metaPreviousVersion = getClient().getObjectMetadataPreviousVersion(test.bucketName, test.objectName);
									
						if (metaPreviousVersion==null) 
							error(" previous version is null -> b:" + test.bucketName + " o:" + test.objectName);
								
						String previousVersionDestFileName = DOWNLOAD_DIR_V0 + File.separator + metaPreviousVersion.fileName;
						try {
							if ((new File(previousVersionDestFileName)).exists())
								FileUtils.forceDelete( new File(previousVersionDestFileName));
							
						} catch (IOException e) {
							error(e);
						}
						
						String currentVersionDestFileName = DOWNLOAD_DIR_V1 + File.separator + metaCurrenVersion.fileName;
						try {
							if ((new File(previousVersionDestFileName)).exists())
								FileUtils.forceDelete( new File(previousVersionDestFileName));
							
						} catch (IOException e) {
							error(e);
						}

						InputStream is0 = null;
						InputStream is1 = null;
						
						try {
							
							is0 = getClient().getObjectPreviousVersion(test.bucketName, test.objectName);
							Files.copy(is0, (new File(previousVersionDestFileName)).toPath(), StandardCopyOption.REPLACE_EXISTING);
							
							is1 = getClient().getObject(test.bucketName, test.objectName);
							Files.copy(is1, (new File(currentVersionDestFileName)).toPath(), StandardCopyOption.REPLACE_EXISTING);
							
							//logger.debug("src v0 -> " + test.getSrcFile(0).getAbsolutePath());
							//logger.debug("src v1 -> " + test.getSrcFile(1).getAbsolutePath());
							
							//logger.debug("dest v0 -> " + previousVersionDestFileName);
							//logger.debug("dest v1 -> " + currentVersionDestFileName);
							
							counterDownloadDataVersion.incrementAndGet();
							
							// validate
							
							try {
								
								String src_sha0 = test.getSrcFileSha256(0);
								String src_sha1 = test.getSrcFileSha256(1);
								
								String dest_sha0 = ODFileUtils.calculateSHA256String(new File(previousVersionDestFileName));
								String dest_sha1 = ODFileUtils.calculateSHA256String(new File(currentVersionDestFileName));
								
								if (!dest_sha0.equals(src_sha0)) {
									error("Error sha256 v0 are not equal -> " + 
											metaPreviousVersion.bucketName + "-" +
											metaPreviousVersion.objectName + 
											" | restored v0 "  + previousVersionDestFileName  + " - " + NumberFormatter.formatFileSize((new File(previousVersionDestFileName)).length()) + " - " + 
											" | src v0 " + test.getSrcFile(0).getName() + " -"  + NumberFormatter.formatFileSize(test.getSrcFile(0).length()));
								}
								
								if (!dest_sha1.equals(src_sha1)) {
									error("Error sha256 v1 are not equal -> " + 
											metaPreviousVersion.bucketName + "-" +
											metaPreviousVersion.objectName + 
											" | dn v1 "  + NumberFormatter.formatFileSize((new File(currentVersionDestFileName)).length()) + " - " + 
											" | src v1 " + NumberFormatter.formatFileSize(test.getSrcFile(1).length()));
								}
								
							} catch (NoSuchAlgorithmException | IOException e) {
								error(e);
							}
							
						} catch (ODClientException e) {
							error(e.getClass().getName() + " | " + e.getMessage());
						}
						finally {
							
							if (is0!=null)
								is0.close();
							
							if (is1!=null)
								is1.close();
						}
					}
					
					// sleep();
					
					/** display status every 3 seconds or so */
					
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.debug( "downloadObjectDataVersions -> " + String.valueOf(counterDownloadDataVersion.get()));
						showStatus = OffsetDateTime.now();
					}
					
				} catch (IOException e) {
					logger.error(e);
					error(e);
					
				} catch (ODClientException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
		}); 
			
		logger.debug( "downloadObjectDataVersions total -> (" + String.valueOf(counterDownloadDataVersion.get()+")"));
		logger.debug("downloadObjectDataVersions ok");
		getMap().put("downloadObjectDataVersions", "ok");
		
		return true;
		
	}
	
	
	/**
	 * 
	 * 
	 * @return
	 */
	
	private boolean getObjectMetadataPreviousVersion() {
		
		counterObjectMetadataPreviousVersion.set(0);
		
		testFiles.forEach((k,test) -> 
		{
				try {
					
					if ( getClient().hasVersions(test.bucketName, test.objectName)) {
						ObjectMetadata meta = getClient().getObjectMetadataPreviousVersion(test.bucketName, test.objectName);
						counterObjectMetadataPreviousVersion.incrementAndGet();
						if (meta==null) 
							error(" previous version is null -> b:" + test.bucketName + " o:" + test.objectName);
					
						ObjectMetadata original = testFiles.get(test.bucketName+"-"+test.objectName).getMetadata(meta.version);
						
						
						/** validate metadata  **/
						
						if (original.hashCode()!=meta.hashCode()) {
							
							logger.debug("original -> " + original.toString());
							logger.debug("meta -> " + meta.toString());
							error(test.bucketName+"-"+test.objectName +" ->  hashcode error");
						}
					}
		
					sleep();
					
					/** display status every 3 seconds or so */
					
					if ( dateTimeDifference( showStatus, OffsetDateTime.now(), ChronoUnit.MILLIS)>THREE_SECONDS) {
						logger.debug( "getObjectMetadataPreviousVersion -> " + String.valueOf(counterObjectMetadataPreviousVersion.get()));
						showStatus = OffsetDateTime.now();
					}
					
				} catch (IOException e) {
					logger.error(e);
					error(e);
					
				} catch (ODClientException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
				}
		}); 
			
		logger.debug( "getObjectMetadataPreviousVersion total -> (" + String.valueOf(counterObjectMetadataPreviousVersion.get()+")"));
		logger.debug("getObjectMetadataPreviousVersion ok");
		
		getMap().put("getObjectMetadataPreviousVersion", "ok");
		return true;
	}

	/**
	 * 
	 */
	protected boolean validateSetUploadNewVersion(Map<String, TestFile> mv) {
		
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
				
					String src_sha = v. getSrcFileSha256(1);
					String new_sha = ODFileUtils.calculateSHA256String(new File(destFileName));
					
					if (!src_sha.equals(new_sha)) {
						logger.error("sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
						error("Error sha256 are not equal -> " + meta.bucketName+"-"+meta.objectName);
					}
					
			} catch (NoSuchAlgorithmException | IOException e) {
					error(e.getClass().getName() + " | " + e.getMessage());
			}
		});
						
		logger.debug("validateSet", "ok");
		getMap().put("validateSet", "ok");
		
		return true;
	}


}
