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

import java.util.List;

import org.junit.Assert;

import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.test.base.BaseTest;

/**
 * 1. create a bucket that does not exists -> must return ok
 * 2. remove a bucket that does not exists -> must throw exception 
 * 3. remove a bucket that does exists and is empty-> must return ok
 * 4. remove a bucket that does exists and is not empty-> must throw exception
 * 5. create a bucket that exists -> error -> must throw exception
 *  
 *  @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class TestBucketCRUD extends BaseTest {
		
	private static final Logger logger = Logger.getLogger(TestBucketCRUD.class.getName());

	private String buckets[] = {"bucket1", "bucket2", "bucket3", "bucket4", "bucket5"};
	private String bucketEmpty;

	
	
	public TestBucketCRUD() {
		logger.debug("Start " + this.getClass().getName());
	}

	@Override
	public void executeTest() {
	 
		try {
			String p=ping();
			if (p==null || !p.equals("ok"))
				throw new RuntimeException("ping error -> " + p!=null?p:"null");
			else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		
		  if (!makeBuckets())
	        	error("makeBuckets");
		  
		if (!listBuckets())
        	error("listBuckets");
	
	     if (!testCreateRemoveCreateBucketEmtpy())
	        error("testCreateRemoveCreateBucketEmtpy");
	     
        if (!testCreateBucketDoesNotExist())
        	error("testCreateBucketDoesNotExist");
        
        if (!testRemoveBucketEmtpy())
        	error("testRemoveBucketEmtpy");

        if (!testRemoveAllBucketsEmtpy())
        	error("testRemoveAllBucketsEmtpy");

        if (!testRemoveBucketDoesNotExist())
        	error("testRemoveBucketDoesNotExist");
        
        
        showResults();
	}
	

	private boolean testCreateRemoveCreateBucketEmtpy() {
	    this.bucketEmpty = "bucket"+randomString(12);
        try {
	    
        	getClient().createBucket(bucketEmpty);
		    Assert.assertTrue(getClient().existsBucket(bucketEmpty));
        	
		    getClient().deleteBucket(bucketEmpty);
		    Assert.assertFalse(getClient().existsBucket(bucketEmpty));

		    getClient().createBucket(bucketEmpty);
		    Assert.assertTrue(getClient().existsBucket(bucketEmpty));
		    
		    logger.debug("testCreateRemoveCreateBucketEmtpy -> ok");
			getMap().put("testCreateRemoveCreateBucketEmtpy", "ok");
	    	return true;
	    	
        }  catch (Exception e) {
        	error(e);
        	return false;
    	}
	}

	
								
	private boolean testCreateBucketDoesNotExist() {
	    this.bucketEmpty = "bucket"+randomString(12);
        try {
	    	getClient().createBucket(bucketEmpty);
	    	logger.debug("testCreateBucketDoesNotExist -> ok");
			getMap().put("testCreateBucketDoesNotExist", "ok");
	    	return true;
        }  catch (Exception e) {
        	error(e);
    	}
        return false;
	}
	
	private boolean testRemoveAllBucketsEmtpy() {
		
		try {
        		for (Bucket bucket:getClient().listBuckets()) {
        			if (getClient().isEmpty(bucket.getName())) {
        				getClient().deleteBucket(bucket.getName());
        			}
        		}
        		logger.debug("testRemoveAllBucketsEmtpy -> ok");
        		getMap().put("testRemoveAllBucketsEmtpy", "ok");
        		return true;
        }  catch (Exception e) {
    		error(e);
    	}
		return false;
		
		
	}
	
	
	private boolean testRemoveBucketEmtpy() {
		try {
        	if (bucketEmpty != null) { 
        		getClient().deleteBucket(bucketEmpty);
        		logger.debug("testRemoveBucketEmtpy -> ok");
        		getMap().put("testRemoveBucketEmtpy", "ok");
        		return true;
        	}
        	else {
        		throw new RuntimeException("bucket is null but it must exist here. aborting");
        	}
        	
        }  catch (Exception e) {
    		error(e);
    	}
		return false;	
	}

	
	private boolean testRemoveBucketDoesNotExist() {
		
	    String b_doesNotExist = "bucket"+randomString(12);
	    
		try {
			getClient().deleteBucket(b_doesNotExist);
			logger.error("must have thrown exception here");
			return false;
        	
        }  catch (ODClientException e) {
        	logger.debug("This exception is ok -> " + e.toString());
        	logger.debug("testRemoveBucketDoesNotExist -> ok");
        	getMap().put("testRemoveBucketDoesNotExist", "ok");
    		return true;
    	}	
	}
	

 

	/**
	 * 
	 */
	public boolean makeBuckets() {
		
			/**------------------
			 * remove buckets if exist
			 */
			try {
				logger.debug("Total buckets -> " + getClient().listBuckets().size());
				
				for (String s: buckets) {
					if (getClient().existsBucket(s))  {
						if (getClient().isEmpty(s)) { 
							logger.debug("removing bucket -> " + s);
							getClient().deleteBucket(s);
							org.junit.Assert.assertFalse(getClient().existsBucket(s));
						}
					}
				}
			} catch (Exception e) {
				error(e);
			}
	    	
			/**------------------
			 * create buckets
			 */
			try {
				int n = 0;
				for (String s: buckets) {
					if (!getClient().existsBucket(s)) {
						getClient().createBucket(s);
						n++;
						org.junit.Assert.assertTrue(getClient().existsBucket(s));
					}
				}
				logger.debug("created -> " + String.valueOf(n) + " buckets");
				getMap().put("makeBuckets", "ok");
				return true;
					
			} catch (Exception e) {
				error(e);
			}
	
			return false;
	}

	
	/**
	 * @return
	 */
	public boolean listBuckets() {
        try {
	        List<Bucket> list = getClient().listBuckets();
	        Assert.assertNotNull(list);
	        for (Bucket bucket: list) {
	        	logger.debug(bucket.getName());
	        }
	        getMap().put("listBuckets", "ok");
	        return true;
	        
        }  catch (Exception e) {
    		error(e);
    	}
        return false;
	}

	
	
	
}
