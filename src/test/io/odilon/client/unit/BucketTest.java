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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.junit.Assert;

import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.test.base.BaseTest;

public class BucketTest extends BaseTest {
	
	private static final Logger logger = Logger.getLogger(BucketTest.class.getName());
	private Map<String, String> map;
    
	public BucketTest() {
		logger.debug("Start " + this.getClass().getName());
		map = new HashMap<String, String>();
	}
	
	@Test	
	public void executeTest() {
	    	
        logger.debug(" ping -------------------------------");
        Assert.assertEquals("ok", super.ping());
        map.put("ping", "ok");
        
    	makeBucket();
    	listBuckets();
    	metrics();
    	
        logger.debug("------------------------------------");
        map.forEach((k,v) -> logger.debug(k+" -> "+ v));
        logger.debug("done -------------------------------");
	}
	

	public void makeBucket() {
        	
        	try {		
        		logger.debug("make bucket -------------------------------");
        		
        		String bucketName = "bucket"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
        		
        		List<Bucket> list = getClient().listBuckets();
        		final int initialSize = list.size();
        		
	        	boolean exists;
		    	exists = getClient().existsBucket(bucketName);
		    	Assert.assertFalse(exists);
		    	logger.debug(bucketName + " exists -> " +  (exists ? "yes": "no"));
		    	
		    	logger.debug("create " + bucketName);
		    	getClient().createBucket(bucketName);
		    	Assert.assertTrue(getClient().existsBucket(bucketName));
		    	
		    	Bucket bucket = getClient().getBucket(bucketName);
			    Assert.assertNotNull(bucket);
			    Assert.assertEquals(initialSize+1,getClient().listBuckets().size());
			    Assert.assertTrue(getClient().isEmpty(bucketName));
			    map.put("makeBucket", "ok");
			    
				logger.debug("remove bucket -------------------------------");
			    logger.debug("delete " + bucketName);
			    getClient().deleteBucket(bucketName);
		    	Assert.assertFalse(getClient().existsBucket(bucketName));
			    Assert.assertEquals(initialSize,getClient().listBuckets().size());
			    map.put("removeBucket", "ok");
		    	
	        } catch (Exception e) {
	    		logger.error(e);
	    		System.exit(1);
	    	}


	}
	public void metrics() {
        	try {
        		logger.debug("metrics -------------------------------");
        		MetricsValues metrics = getClient().metrics();
        		Assert.assertNotNull(metrics);
        		logger.debug(metrics.toString());
        	    map.put("metrics", "ok");
        		
	    	} catch (Exception e) {
	    		logger.error(e);
	    		System.exit(1);
	    	}
	}

	public void listBuckets() {
	    logger.debug("list buckets -------------------------------");
        try {
	        List<Bucket> list = getClient().listBuckets();
	        Assert.assertNotNull(list);
	        for (Bucket bucket: list) {
	        	logger.debug(bucket.toString());
	        }
	        map.put("listBuckets", "ok");
        }  catch (Exception e) {
    		logger.error(e);
    		System.exit(1);
    	}
		
	}
	public void getBucket() {
		try {							
    		logger.debug("get bucket -------------------------------");
    		List<Bucket> list = getClient().listBuckets();
            String bucketName = list.get(0).getName();
            Bucket bucket = getClient().getBucket(bucketName);
            Assert.assertNotNull(bucket);
            Assert.assertTrue(bucket.getName().equals(bucketName));
           	logger.debug(bucket.getName());
           	map.put("getBuckets", "ok");
           	
		} catch (Exception e) {
			logger.error(e);
    		System.exit(1);
		}
	}

	/**
	public void other() {
        	try {
		        	logger.debug("check if bucket exists -------------------------------");
		            List<Bucket> list = client.listBuckets();
		            Bucket bucket = list.get(0);
		            String bucketName = bucket.getName();
			        boolean exists;
			    	exists = client.bucketExists(bucketName);
			    	Assert.assertTrue(exists);
			    	logger.debug("bucket1. exists ->" +  (exists ? "yes": "no"));
			    	String bucketNotExists=bucketName+"doesnotexist";
			    	try {
				        exists = client.bucketExists(bucketNotExists);
			    	} catch (ODClientException e) {
			    		Assert.assertFalse(exists);
				        logger.debug(bucketNotExists+". exists ->" +  (exists ? "yes": "no"));
		               	map.put("bucketExists", "ok");

			    	}
			        
        	} catch (Exception e) {
	    		logger.error(e);
	    		System.exit(1);
	    	} 
    	}
        
        {				
        	try {							
        		logger.debug("get bucket -------------------------------");
        		List<Bucket> list = client.listBuckets();
                String bucketName = list.get(0).getName();
                
                Bucket bucket = client.getBucket(bucketName);
	    	
                Assert.assertNotNull(bucket);
                Assert.assertTrue(bucket.getName().equals(bucketName));
               	logger.debug(bucket.getName());
               	map.put("getBucket", "ok");
        		} catch (Exception e) {
        			logger.error(e);
    	    		System.exit(1);
        		}
        }
        
    
        
        {
        	try {	
        		logger.debug("Object Metadata -------------------------------");
        		String bname = "bucket1";
			      String oname = "objectName-0";
			      logger.debug("get -> b:" + bname + " o:" + oname);
			      ObjectMetadata meta = client.getObjectMetadata(bname, oname);
			      Assert.assertNotNull(meta);
			      if (meta!=null) {
			    	  logger.debug(meta.toString());
			    	  Assert.assertEquals(bname, meta.bucketName);
			    	  Assert.assertEquals(oname, meta.objectName);
					    map.put("getObjectMetadata", "ok");
			      }
        	} catch (Exception e) {
        		logger.error(e);
        		System.exit(1);
        	}
        

        
	}
	*/

}
