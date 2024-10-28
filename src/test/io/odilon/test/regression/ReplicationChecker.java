package io.odilon.test.regression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SharedConstant;
import io.odilon.model.list.DataList;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;

public class ReplicationChecker extends BaseTest {
	
	private static final Logger logger = Logger.getLogger(ReplicationChecker.class.getName());
	
	
	@Override
	public void executeTest() {
		checkBuckets(); 
		
		checkObjects();
		
		showResults();
		

	}
	
	/**
	 * 
	 * 
	 */
	protected void checkBuckets() {

		List<String> bucketsLocalNotRemote = new ArrayList<String>();
		List<String> bucketsRemoteNotLocal = new ArrayList<String>();
		
		try {
			for (Bucket bucket: getClient().listBuckets()) {
					if (!getStandByClient().existsBucket(bucket.getName())) {
						bucketsLocalNotRemote.add(bucket.getName());
					}
			}
		} catch (ODClientException e) {
			logger.error(e);
			error(e);
		}
		
		
		try {
				for (Bucket bucket: getStandByClient().listBuckets()) {
					if (!getClient().existsBucket(bucket.getName())) {
						bucketsRemoteNotLocal.add(bucket.getName());
				}
			}
			
		} catch (ODClientException e) {
			logger.error(e);
			error(e);
		}
		
		if ((bucketsLocalNotRemote.size()==0) && (bucketsRemoteNotLocal.size()==0)) {
			logger.info("checkBuckets ok");
			getMap().put("checkBuckets", "ok");
		}
		else {
			logger.error("checkBuckets error");
			
			logger.error("bucketsLocalNotRemote: ");
			bucketsLocalNotRemote.forEach(n -> logger.error(n));
			
			logger.error("bucketsRemoteNotLocal: ");
			bucketsRemoteNotLocal.forEach(n -> logger.error(n));
			
			error("checkBuckets error");
		}
		
	}
	

	protected void checkObjects() {
		
		try {
			for (Bucket bucket: getClient().listBuckets()) {
				 checkBucket(bucket); 
			}
		} catch (ODClientException e) {
			logger.error(e);
			error(e);
		}
	}
	
	
	static final int PAGESIZE = SharedConstant.DEFAULT_PAGE_SIZE;
	
	protected void checkBucket(Bucket bucket) {
		
		List<String> localNotRemote = new ArrayList<String>();
		List<String> remoteNotLocal = new ArrayList<String>();
		
		List<String> versionDiffs = new ArrayList<String>();
		
		List<String> errors = new ArrayList<String>();

		logger.debug("");
		logger.debug("Checking Bucket -> " + bucket.getName());

		
		{
				
				ResultSet<Item<ObjectMetadata>> data;
				
				try {
					
					data = getClient().listObjects(bucket.getName());
					
					int counter = 0;
					
					while (data.hasNext()) {
						Item<ObjectMetadata> item = data.next();
						if (item.isOk()) {
							try {
								if (!getStandByClient().existsObject(item.getObject().bucketName, item.getObject().objectName)) {
									localNotRemote.add("b:" + item.getObject().bucketName + " o:" + item.getObject().objectName);
								}
								else {
									
									ObjectMetadata metaLocal   = getClient().getObjectMetadata(item.getObject().bucketName, item.getObject().objectName);
									ObjectMetadata metaRemote  = getStandByClient().getObjectMetadata(item.getObject().bucketName, item.getObject().objectName);
									
									if (metaLocal.version!=metaRemote.version) {
										versionDiffs.add("l_v: " + String.valueOf(metaLocal.version) + " r_v: " + String.valueOf(metaRemote.version) +" | b:" + item.getObject().bucketName + " o: " + item.getObject().objectName);
									}
									
									if (!metaLocal.fileName.equals(metaRemote.fileName)) {
										versionDiffs.add("l_fn: " + String.valueOf(metaLocal.fileName) + " r_fn: " + String.valueOf(metaRemote.fileName) +" | b:" + item.getObject().bucketName + " o: " + item.getObject().objectName);
									}
									
									if (metaLocal.length!=metaRemote.length) {
										versionDiffs.add("l_lentgth: " + String.valueOf(metaLocal.length) + " r_length: " + String.valueOf(metaRemote.length) +" | b:" + item.getObject().bucketName + " o: " + item.getObject().objectName);
									}
									
									
									if (getClient().isVersionControl() && getStandByClient().isVersionControl()) {
										if (getClient().hasVersions(item.getObject().bucketName, item.getObject().objectName) != getStandByClient().hasVersions(item.getObject().bucketName, item.getObject().objectName) ) {
											versionDiffs.add("l_hasVersions: " + String.valueOf(getClient().hasVersions(item.getObject().bucketName, item.getObject().objectName)) + 
															 "r_hasVersions: " + String.valueOf(getStandByClient().hasVersions(item.getObject().bucketName, item.getObject().objectName)) +
															 " | b:" + item.getObject().bucketName + " o: " + item.getObject().objectName);
										}
										logger.debug(String.valueOf(++counter) +  ".  b:" + item.getObject().bucketName + " o: " + item.getObject().objectName);
									}
									
								}
							} catch (Exception e) {
								errors.add(e.getClass().getName());
							}
						}
					}
					
				} catch (ODClientException e) {
					error(e);
				}
			}
		
		

		
		
		boolean error = false;
		if (localNotRemote.size()>0) {
			localNotRemote.forEach(n -> logger.error(n));
			error=true;
		}
		
		if (remoteNotLocal.size()>0) {
			remoteNotLocal.forEach(n -> logger.error(n));
			error=true;
		}
		
		if (versionDiffs.size()>0) {
			versionDiffs.forEach(n -> logger.error(n));
			error=true;
		}
		
		
		if (error) {
			error("checkBuckets error");
		}
		else {
			logger.info("checkBucket b:" + bucket.getName() + " ok");
			getMap().put("checkBucket b:" + bucket.getName(), "ok");
		}
		
	}
	
	

	
}
