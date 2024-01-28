package io.odilon.client.unit;

import io.odilon.client.error.ODClientException;
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
	
	public TestPresignedUrl() {
	}
	
	static int MAX = 100;
	
	@Override
	public void executeTest() {

		try {
			if (getClient().listBuckets().isEmpty())
				error("must have at least 1 bucket");
			
			org.junit.Assert.assertFalse("must have at least 1 bucket", getClient().listBuckets().isEmpty());
			
			this.bucket_1 = getClient().listBuckets().get(0);
			org.junit.Assert.assertFalse("bucket must not be empty", getClient().isEmpty(this.bucket_1.getName()));
			
			 ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
			 int counter = 0;
			 while (rs.hasNext() && counter++ < MAX) {
				 Item<ObjectMetadata> item = rs.next();
				 if (item.isOk()) {
					 	ObjectMetadata meta = item.getObject();
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> " + getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName)) ;	 
				 }
			 }
			 
			getMap().put("presigned test -> " + String.valueOf(counter), "ok");
			showResults();
			 
			 
		} catch (ODClientException e) {
			error(e);
		}
	}
}









