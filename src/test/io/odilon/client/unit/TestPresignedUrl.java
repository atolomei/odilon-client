package io.odilon.client.unit;


import java.io.File;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;


/**
 * <p>A presigned URL is a way to grant temporary access to an Object, for example in an HTML webpage.
   It remains valid for a limited period of time which is specified when the URL is generated.
 * </p>
 *  
 *
 */
public class TestPresignedUrl extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());

	private Bucket bucket_1;
	private String bucketName = null;
	
	public TestPresignedUrl() {

	}
	
	
	
	@Override
	public void executeTest() {

		try {
			if (getClient().listBuckets().isEmpty()) {
				createBucket();
				addFiles();
			}
			
			org.junit.Assert.assertFalse("must have at least 1 bucket", getClient().listBuckets().isEmpty());
			
			this.bucket_1 = getClient().listBuckets().get(0);
			org.junit.Assert.assertFalse("bucket must not be empty", getClient().isEmpty(this.bucket_1.getName()));
			
			 ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
			 int counter = 0;
			 int total = 0;
			 
			 while (rs.hasNext() && counter++ < getMax()) {
				 Item<ObjectMetadata> item = rs.next();
				 if (item.isOk()) {
					 	ObjectMetadata meta = item.getObject();
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> " + getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName)) ;
						total++;
				 }
			 }
			 
			 if (bucketName!=null) {
				 // delete all
				 // delete bucket
			 }
				 
			getMap().put("presigned test -> " + String.valueOf(total), "ok");
			showResults();
			 
			 
		} catch (ODClientException e) {
			error(e);
		}
	}


	
	
	private void createBucket() {
		
		bucketName = randomString(10);
		
		try {
		
			getClient().createBucket(bucketName);
			
		} catch (ODClientException e) {
			error(e);
		}
		
	}
	
	
	/**
	 * 
	 * 
	 * @return
	 */
	private boolean addFiles() {
		
        File dir = new File(SRC_DIR_V0);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +SRC_DIR_V0);
		}
        
		int counter = 0;
		
		String bucketName = null;
		bucketName = this.bucket_1.getName();
			
		
		// put files
		//
		for (File fi:dir.listFiles()) {
			
			if (counter >= getMax())
				break;
			
			if (isElegible(fi)) {
				String objectName = FSUtil.getBaseName(fi.getName())+"-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
				try {
					getClient().putObject(bucketName, objectName, fi);
					counter++; 
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				}
			}
		}
		
		return true;
	
	}	

	
}









