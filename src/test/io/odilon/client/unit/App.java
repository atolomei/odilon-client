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

import io.odilon.client.ODClient;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.model.ObjectMetadata;


/**
 *
 */
public class App 
{
	
	private static final Logger logger = Logger.getLogger(App.class.getName());
	
	static final String TEMP_DIR = "c:"+File.separator+"temp";
			
    public static void main( String[] args )
    {

    	logger.debug("Start app");
        
        String endpoint = "http://localhost";
        int port = 9200;
        String accessKey = "odilon";
        String secretKey = "odilon";
        boolean secure = false;

        {
            logger.debug(" starting App -------------------------------");
        }

        
        ODClient client = new ODClient(endpoint, port, accessKey, secretKey, secure);
        logger.debug(client.toString());


        {				
            logger.debug(" ping -------------------------------");
            logger.debug(client.ping());
        }
        
        
        
        {

        	try {	
		    	  String bname = "bucket1";
			      String oname = "objectName-0";
			      logger.debug("get -> b:" + bname + " o:" + oname + " -------------------------------");
			      ObjectMetadata meta = client.getObjectMetadata(bname, oname);
			      if (meta!=null) {
			    	  logger.debug(meta.toString());
			  
			    	  //try {
			    	//		client.getObject(bname, oname, TEMP_DIR + File.separator + meta.filename);
			    	//		
			    	//	} catch (IOException e) {
			    	//		logger.error(e);
			    	//		System.exit(1);
			    	//	}
			      } else {
			    	  
			    	  logger.error("meta is null");
			      }
        	} catch (Exception e) {
        		logger.error(e);
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
	    	}
        }

        
        
        
        {				
        	String bucketNotExist = "bucket-"+String.valueOf(Double.valueOf((Math.abs(Math.random()*100000))).intValue());
        	logger.debug("get bucket not exist -------------------------------");
        	try {
		    
        		Bucket bucket = client.getBucket(bucketNotExist);
			    logger.debug(bucket.getName());
		    	
	    	} catch (Exception e) {
	    		logger.error(e);
	    	}
        }

        {
    	 
        	try {
        		logger.debug("metrics -------------------------------");
        		MetricsValues metrics = client.metrics();
        		logger.debug(metrics.toString());
	    	} catch (Exception e) {
	    		logger.error(e);
	    	}
        }
        
     /* {
	        	String bname = "bucket1";
	        	String oname = "objectName-0";
	        	
	    		InputStream is = client.getObject(bname, oname);

	    		try {
					
	    			is.close();
	    			
				} catch (IOException e) {
					logger.error(e);

				}
        }
        */
      
	    
        //{
	    //  logger.debug("status -------------------------------");
	    //  Map<String, Object> status = client.status();
	    //  logger.debug(status.toString());
        // }
        
        
      
        {
        	try {
        	logger.debug("check if bucket exists -------------------------------");
	    	boolean exists;
	    	exists = client.existsBucket("bucket1");
	        logger.debug("bucket1. exists ->" +  (exists ? "yes": "no"));
	    	exists = client.existsBucket("bucketnoe");
	        logger.debug("bucketnoe. exists ->" +  (exists ? "yes": "no"));
        	} catch (Exception e) {
	    		logger.error(e);
	    	} 
    	}
        

        {				
	        
        	try {
        		logger.debug("get bucket1 -------------------------------");
        	
	        Bucket bucket = client.getBucket("bucket1");
	    	if (bucket!=null) {
	        	logger.debug(bucket.getName());
	    	}
        	} catch (Exception e) {
	    		logger.error(e);
	    	}
        }
        

        {
        	
        	try {
        		String bname = "bucketcnt2";
	        	boolean exists;
		    	exists = client.existsBucket(bname);
		    	logger.debug(bname + " exists -> " +  (exists ? "yes": "no"));
		    	
		    	if (!exists) {
		    		logger.debug("create " + bname + " -------------------------------");
		        	client.createBucket(bname);
			        Bucket bucket = client.getBucket(bname);
			        if (bucket!=null) {
			        	logger.debug(bucket.toString());
			        }
			        else
			        	logger.debug("bucket not found -> " + bname);
		    	}
	        } catch (Exception e) {
	    		logger.error(e);
	    	}
        }
        
        
        {
        	try {	
        	String bname = "bucketcnt2";
	        	boolean exists;
		    	exists = client.existsBucket(bname);
		    	logger.debug(bname + " exists -> " +  (exists ? "yes": "no"));
		    	
		    	if (exists) {
		    		logger.debug("remove " + bname + " -------------------------------");
		        	client.deleteBucket(bname);
		        	exists = client.existsBucket(bname);
			    	logger.debug(bname + " exists -> " +  (exists ? "yes": "no"));
		    	}
	        } catch (Exception e) {
	    		logger.error(e);
	    	}

        }
        
              logger.debug("done");
        
    }
}
