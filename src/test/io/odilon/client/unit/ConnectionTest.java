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
import java.util.List;

import org.junit.jupiter.api.Test;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.unit.App;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import junit.framework.Assert;

public class ConnectionTest {

	private static final Logger logger = Logger.getLogger(ConnectionTest.class.getName());
	
	static final String TEMP_DIR = "c:"+File.separator+"temp";
	
	String endpoint = "http://localhost";
    int port = 9200;
    String accessKey = "odilon";
    String secretKey = "odilon";
    
	public ConnectionTest() {
		logger.debug("Start " + this.getClass().getName());
	}
	
	@Test	
	public void testConnection() {
		
		//ODClient client = new ODClient(endpoint, port, accessKey, secretKey);
		
		var client = new ODClient(endpoint, port, accessKey, secretKey);
        logger.debug(client.toString());
    
        {				
            logger.debug(" ping -------------------------------");
            logger.debug(client.ping());
            Assert.assertEquals("ok", client.ping());
            
        }
        {
        	try {	
		    	  String bname = "bucket1";
			      String oname = "objectName-0";
			      logger.debug("get -> b:" + bname + " o:" + oname + " -------------------------------");
			      ObjectMetadata meta = client.getObjectMetadata(bname, oname);
			      if (meta!=null) {
			    	  logger.debug(meta.toString());
			    	  //Assert.assertEquals("ok", client.ping());
			    	  
			      } else {
			    	  logger.error("meta is null for -> b:" + bname + " o:" + oname);
			      }
        	} catch (Exception e) {
        		logger.error(e);
        		System.exit(1);
        	}
	     }
        
        
        {
	        logger.debug("list buckets -------------------------------");
	        try {
		        List<Bucket> list = client.listBuckets();
		        for (Bucket bucket: list) {
		        	logger.debug(bucket.toString());
		        }
	        }  catch (Exception e) {
	    		logger.error(e);
	    		System.exit(1);
	    	}
        }

        
        
        {
        	try {
		        	logger.debug("check if bucket exists -------------------------------");
		            List<Bucket> list = client.listBuckets();
		            Bucket bucket = list.get(0);
		            String bucketName = bucket.getName();
			        boolean exists;
			    	exists = client.existsBucket(bucketName);
			    	Assert.assertTrue(exists);
			    	logger.debug("bucket1. exists ->" +  (exists ? "yes": "no"));
			    	
			    	String bucketNotExists=bucketName+"doesnotexist";
			    	try {
				        exists = client.existsBucket(bucketNotExists);
			    	} catch (ODClientException e) {
			    		Assert.assertFalse(exists);
				        logger.debug(bucketNotExists+". exists ->" +  (exists ? "yes": "no"));
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
                
        		} catch (Exception e) {
        			logger.error(e);
    	    		System.exit(1);
        		}
        }
        
        {
        	
        	try {		
        		logger.debug("make bucket -------------------------------");
        		
        		String bucketName = "bucket-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
        		
        		List<Bucket> list = client.listBuckets();
        		final int initialSize = list.size();
        		
	        	boolean exists;
		    	exists = client.existsBucket(bucketName);
		    	Assert.assertFalse(exists);
		    	logger.debug(bucketName + " exists -> " +  (exists ? "yes": "no"));
		    	
		    	logger.debug("create " + bucketName);
		    	client.createBucket(bucketName);
		    	Assert.assertTrue(client.existsBucket(bucketName));
			    
		    	Bucket bucket = client.getBucket(bucketName);
			    Assert.assertNotNull(bucket);
			    Assert.assertEquals(initialSize+1,client.listBuckets().size());
			    Assert.assertTrue(client.isEmpty(bucketName));
			    
			    
			    
			    logger.debug("delete " + bucketName);
			    client.deleteBucket(bucketName);
		    	Assert.assertFalse(client.existsBucket(bucketName));
			    Assert.assertEquals(initialSize,client.listBuckets().size());
			    
		    	
	        } catch (Exception e) {
	    		logger.error(e);
	    	}
        }

        			
        logger.debug("done -------------------------------");
        
        
        
        
        
        
        
        
        
        
        
        
	}

}
