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

import java.util.List;

import io.odilon.client.ODClient;
import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.model.Bucket;

public class SampleListBuckets {
			
	private String endpoint = "http://localhost";
	private int port = 9200;
	private String accessKey = "odilon";
	private String secretKey = "odilon";
	
	private OdilonClient client;
	
	public SampleListBuckets() {
	}
	 
	public void list() {
		 
		 client = new ODClient(endpoint, port, accessKey, secretKey);
		 
		 String ping =client.ping();
		 
		 if (!ping.equals("ok")) {
			 System.out.println("ping error -> " + ping);			 
			 System.exit(1);
		 }
		 
		 try {
		
			 List<Bucket> listBuckets = client.listBuckets();
			 listBuckets.forEach( item -> System.out.println(item.toString()) );
		
		
		} catch (ODClientException e) {
			System.out.println(e.getClass().getName() + " " + e.getMessage());
		}
	 }
	 
	 
	 public static void main(String [] args) {
		 
		 System.out.println("Starting " + SampleListBuckets.class.getName() );
		 
		 SampleListBuckets listBuckets = new SampleListBuckets();
		 
		 listBuckets.list();
		 
		 System.out.println("done." );
	 }
	 

	 
}
