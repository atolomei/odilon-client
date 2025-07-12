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
package io.odilon.demo;

import java.util.Optional;

import io.odilon.client.ODClient;
import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;

/**
 * <p>A presigned URL is a way to grant temporary access to an Object, for example in an HTML webpage.
   It remains valid for a limited period of time which is specified when the URL is generated.
 * </p>
 * 
 *
 */
public class SamplePresignedUrl {

	private static final Logger logger = Logger.getLogger(SamplePresignedUrl.class.getName());

	static int MAX = 100;
	
	private Bucket bucket;
	
	private String endpoint = "http://localhost";
	private int port = 9234;
	private String accessKey = "odilon";
	private String secretKey = "odilon";
    
	private OdilonClient client;
	

	public SamplePresignedUrl() {
		try {
			
			this.client = new ODClient(endpoint, port, accessKey, secretKey);
			
		} catch (Exception e) {
		    System.out.println(e);             
            System.exit(1);
		}
		
	}
	
	
	public void executeTest() {

		try {
			if (getClient().listBuckets().isEmpty()) {
			    System.out.println("must have at least 1 bucket");
			    System.exit(1);
			}
			
			this.bucket = getClient().listBuckets().get(0);
			
			 ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket.getName());
			 int counter = 0;
			 while (rs.hasNext() && counter++ < MAX) {
				 Item<ObjectMetadata> item = rs.next();
				 if (item.isOk()) {
					 	ObjectMetadata meta = item.getObject();
						
						/** by default the link lasts 7 days */
						logger.debug(meta.bucketName + " / " + meta.objectName + " (7 days) -> " + getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName));	 
						
						/** url valid for 5 minutes */
						logger.debug(
						        meta.bucketName + " / " + meta.objectName + " (5 min) -> " + 
						getClient().getPresignedObjectUrl(   meta.bucketName, meta.objectName, Optional.of(Integer.valueOf(60*5))));	 
				 }
			 }
			 
		} catch (ODClientException e) {
		    System.out.println("must have at least 1 bucket");
            System.exit(1);
		}
	}
	
	public OdilonClient getClient() { 
		return client;
	}
	
    public static void main(String [] args) {
        
        System.out.println("Starting " + SamplePresignedUrl.class.getName() );
        
        SamplePresignedUrl sample = new SamplePresignedUrl();
        
        sample.executeTest();
        
        System.out.println("done." );
    }
    
}









