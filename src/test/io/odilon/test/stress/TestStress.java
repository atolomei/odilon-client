package io.odilon.test.stress;


import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.SharedConstant;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;

public class TestStress extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(TestStress.class.getName());

	static final double KB = SharedConstant.d_kilobyte;
	static final double MB = SharedConstant.d_megabyte;;
	static final double GB = SharedConstant.d_gigabyte;

	private List<String> bannedDirs;
	
	static final int BUFFER_SIZE = 4096;
	static final int MAX = 5000;
	static final long MAX_LENGTH = 10 * 1000 * 1000; // 10 MB
	
										
	private Map<String, TestFile> testFiles;
	private List<File> up;
	private Bucket bucket_1 = null;
	private boolean done = false;
	private long totalSize = 0;

	// private AtomicLong total_uploaded = new AtomicLong(0);
	// private AtomicLong totalUpload_bytes = new AtomicLong(0);
	// private int maxUploadingThread  = 6;
	
	/**
	 * 
	 */
	@Override
	public void executeTest() {
		
		preCondition();
		
		this.up = new ArrayList<File>();
		testFiles = new ConcurrentHashMap<String, TestFile>();
		bannedDirs = new ArrayList<String>();
		
		// TODO AT
		/**
		bannedDirs.add("c:"+File.separator+"odilon");
		bannedDirs.add("c:"+File.separator+"odilon-data");
		bannedDirs.add("c:"+File.separator+"odilon-data-raid0");
		bannedDirs.add("c:"+File.separator+"odilon-data-raid1");
		bannedDirs.add("c:"+File.separator+"$RECYCLE.BIN");
		**/
		totalSize = 0;
		add(new File(getSourceDir()));
		double val = Double.valueOf(totalSize).doubleValue() / MB;
		
		
		
		
		// --------
		 TestStressUploader uploader = new TestStressUploader(bucket_1, up, testFiles, this);
		 
		 logger.debug("This test will upload files in parallel:");
		 logger.debug("Upload threads  -> " +  String.valueOf(uploader.getUploadingThread()));
		 logger.debug("Files to upload -> " + up.size() + " | " + String.format("%9.2f MB", val).trim());
		 logger.debug("---");
		 
		 try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		 
		 uploader.start();
		 
		// --------
		 /**
		int DOWNLOADERS = 4;
		Downloader dn[] = new Downloader[DOWNLOADERS];
		for (int n=0; n<DOWNLOADERS; n++) {
			dn[n]=new Downloader(Downloader.class.getSimpleName()+"_" + String.valueOf(n), bucket_1, testFiles, this);
			dn[n].start();
		}
		*/
		OffsetDateTime start = OffsetDateTime.now();
		
		this.done = false;
		
		while (!done) {
			try {
				Thread.sleep(2000);
				
				if ((OffsetDateTime.now().isAfter(start.plusMinutes(10)))) 
						done=true;
				
				if (uploader.isDone())
						done=true;
				
				logger.debug("uploaded so far -> " + String.valueOf(uploader.getTotalUploaded().get()));
			} catch (InterruptedException e) {
			}
		}
		
		uploader.sendExitSignal();
		
		
		/**
		for (int n=0; n<DOWNLOADERS; n++) {
			dn[n].sendExitSignal();
		}
		**/
		
		
		// ------------------
		// while (!done) {
		//	 try {
		//		Thread.sleep(2000);
		//	} catch (InterruptedException e) {
		//	}
		//	 done = downloader.exit();
		// }
		// ------------------
		 
		/**
			 TestStressDownload downloader = new TestStressDownload(bucket_1, testFiles, this);
			 downloader.run();
			 TestStressDelete deleter = new TestStressDelete(bucket_1, testFiles, this);
			 deleter.run();
		*/
		
	}
	
	
	
	private void add(File dir) {
		
		if (!dir.isDirectory()) 
			throw new IllegalArgumentException("Directotry -> " + dir.getAbsolutePath());
		
		if (bannedDirs.contains(dir.getAbsolutePath()))
				return;
		
		if (!dir.canRead())
			return;
				
		if (dir.listFiles()==null) {
			return;
		}

		for (File file: dir.listFiles()) {
				
				if (done)
					return;
				
				if (file.isDirectory()) {
					add(file);
				}
				else {
					if (isElegible(file)) {
						up.add(file);
						totalSize += file.length();
						done = (up.size() == getMax());
						if (up.size() % 101 == 0)
							logger.debug(String.valueOf(up.size()) + " | " + file.getAbsolutePath());
					}
				}
			}
	}
	
	/**
	 */
	public void startDownload() {
	}

	/**
	 */
	public void startDelete() {
		
	}

	
	/**
	 */
	public boolean preCondition() {

        File dir = new File( super.getSourceDir());
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " + getSourceDir());
		}

        try {
			String p=ping();
			if (p==null || !p.equals("ok"))
				error("ping  -> " + p!=null?p:"null");
			else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
        
        
        File tmpdir = new File(super.getDownloadDirHeadVersion());
        
        if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
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
       	
       	
       	String bucketTest = "dev-test";
        
        try {	
			if (!getClient().existsBucket(bucketTest)) {
				getClient().createBucket(bucketTest);
			}
			
			bucket_1 = getClient().getBucket(bucketTest);
			
		} catch (ODClientException e) {
			
			error(e.getClass().getName() + " | " + e.getMessage());
		}
       	
       	
		return true;
	
	}
	
}
