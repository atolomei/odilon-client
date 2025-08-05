/*
 * Odilon Object Storage
 * (c) kbee 
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


import java.util.List;

import io.odilon.client.ODClient;
import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;

public class SampleListObjects {


	private String endpoint = "http://localhost";
	private int port = 9200;
	private String accessKey = "odilon";
	private String secretKey = "odilon";

	private OdilonClient client;
	
	public SampleListObjects() {}
	
	public void list() {
		
		client = new ODClient(endpoint, port, accessKey, secretKey);

		/** ping server. If the server is not online, exit */
		 String ping =client.ping();
		 if (!ping.equals("ok")) {
			 System.out.println("ping error -> " + ping);			 
			 System.exit(1);
		 }
		 
		 /** Get a Bucket. if there are none,  create one */
		 Bucket bucket = null;
		
		 try {

			 List<Bucket> listBuckets = client.listBuckets();
			 if (listBuckets.isEmpty())  
				 throw new RuntimeException("there are no buckets");
		 else
			 bucket = listBuckets.get(0);
		 } catch (ODClientException e) {
		    	System.out.println(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				System.exit(1);
		 }
		
		try {
		    	ResultSet<Item<ObjectMetadata>> resultSet = client.listObjects(bucket.getName());
		    	while (resultSet.hasNext()) {
		    		Item<ObjectMetadata> item = resultSet.next();
		    		if (item.isOk())
		    			System.out.println(" objectName:" + item.getObject().objectName +" | file: " + item.getObject().fileName);
		    		else
		    			System.out.println(item.getErrorString());
		    	}
		    } catch (ODClientException e) {
		    	System.out.println(String.valueOf(e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()));
				System.exit(1);
			}
			
	}
		 
	
	public static void main(String [] args) {
		 
		 System.out.println("Starting " + SampleListObjects.class.getName() );
		 SampleListObjects lo = new SampleListObjects();
		 lo.list();
		 System.out.println("done." );
	 }
	
}
