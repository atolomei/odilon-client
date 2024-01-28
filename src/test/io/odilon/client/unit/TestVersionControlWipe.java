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
import io.odilon.test.base.TestFile;
import io.odilon.util.ODFileUtils;

public class TestVersionControlWipe extends BaseTest {

	
	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());
	
	
	private Bucket bucket;
	
	private OffsetDateTime showStatus = OffsetDateTime.now();
	
	
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
			
			logger.debug( "wipeBucket -> ok");
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

		try {
			if (!getClient().isVersionControl())
				error("version control must be enabled");
			
		} catch (ODClientException e) {
			error(e);
		}
		
		
		try {
			
			if (!getClient().existsBucket("test-version-control"))
				error("bucket must exist -> " + "test-version-control");
			
			bucket = getClient().getBucket("test-version-control");
			
			if (getClient().isEmpty(bucket.getName())) {
				error("bucket is empty -> " + bucket.getName());
			}
			
			
		} catch (ODClientException e) {
			error(e);
		}
		
		return true;
	}

	
}	

