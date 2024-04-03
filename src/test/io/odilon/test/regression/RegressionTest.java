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
package io.odilon.test.regression;


import io.odilon.client.unit.TestBucketCRUD;
import io.odilon.client.unit.TestDeleteObject;
import io.odilon.client.unit.TestFileCache;
import io.odilon.client.unit.TestGetObjects;
import io.odilon.client.unit.TestMasterStandBy;
import io.odilon.client.unit.TestMonitor;
import io.odilon.client.unit.TestObjectPutGet;
import io.odilon.client.unit.TestObjectPutVersion;
import io.odilon.client.unit.TestPresignedUrl;
import io.odilon.client.unit.TestQuery;
import io.odilon.client.unit.TestVersionControlUpload;
import io.odilon.client.unit.TestVersionControlWipe;
import io.odilon.log.Logger;
import io.odilon.test.base.BaseTest;


/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class RegressionTest extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(RegressionTest.class.getName());
	
	@Override
	public void executeTest() {
		
		TestBucketCRUD bucket_t = new  TestBucketCRUD();
		bucket_t.setClient(getClient());
		bucket_t.executeTest();
		
		TestObjectPutGet object_t = new  TestObjectPutGet();
		object_t.setClient(getClient());
		object_t.executeTest();
		
		TestMonitor monitor_t = new  TestMonitor();
		monitor_t.setClient(getClient());
		monitor_t.executeTest();
		
		TestGetObjects get_t = new  TestGetObjects();
		get_t.setClient(getClient());
		get_t.executeTest();
		
		TestDeleteObject delete_object_t = new  TestDeleteObject();
		delete_object_t.setClient(getClient());
		delete_object_t.executeTest();
		
		
		TestPresignedUrl presigned_t = new  TestPresignedUrl();
		presigned_t.setClient(getClient());
		presigned_t.executeTest();
		
		TestQuery query_t=new TestQuery();
		query_t.setClient(getClient());
		query_t.executeTest();
		
		if (isVersionControl()) {
			TestObjectPutVersion version_t= new TestObjectPutVersion();
			version_t.setClient(getClient());
			version_t.executeTest();
			
			TestVersionControlUpload upload_t = new TestVersionControlUpload();
			upload_t.setClient(getClient());
			upload_t.executeTest();
			
			TestVersionControlWipe wipe_t=new TestVersionControlWipe();
			wipe_t.setClient(getClient());
			wipe_t.executeTest();
		}
				
		
		if (isRAIDSix()) {
			TestFileCache cache_t = new  TestFileCache();
			cache_t.setClient(getClient());
			cache_t.executeTest();
		}

		
		if (isStandBy()) {
			TestMasterStandBy standby_t = new  TestMasterStandBy();
			standby_t.setClient(getClient());
			standby_t.executeTest();
		}

		logger.debug(this.getClass().getName() + " -> done.");
		
	}


	

}
