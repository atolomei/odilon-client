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
package io.odilon.client.unit;

import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;


public class TestListEncryptedObjects extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestListEncryptedObjects.class.getName());

	public TestListEncryptedObjects() {
	}

	@Override
	public void executeTest() {

		try {

			long total_encrypted = 0;
			long total_total = 0;
			long total_plain = 0;

			for (Bucket bu : getClient().listBuckets()) {

				
				long encrypted = 0;
				long total = 0;
				long plain = 0;
		
				logger.debug( "Bucket -> " + bu.getName() );
		
				ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(bu.getName());

				while (rs.hasNext()) {
					Item<ObjectMetadata> item = rs.next();

					if (item.isOk()) {
						total++;
						ObjectMetadata meta = item.getObject();
						if (meta.isEncrypt()) {
							encrypted++;
							logger.debug(meta.bucketName + " / " + meta.objectName + " - encrypted");
						} else {
							plain++;
						}
					}
				}

				logger.debug(String.valueOf(plain) +" plain  | " + String.valueOf(encrypted) +  " encrypted | " + String.valueOf(total) + " total", "ok");
				logger.debug(bu.toString());
	 
				logger.debug("");
				
				total_encrypted += encrypted;
				total_total += total;
				total_plain += plain;
				
				
			}

			logger.debug("");
			logger.debug("");

			
			getMap().put("plain -> " + String.valueOf(total_plain), "ok");
			getMap().put("encrypted -> " + String.valueOf(total_encrypted), "ok");
			getMap().put("total -> " + String.valueOf(total_total), "ok");
			
			showResults();

		} catch (ODClientException e) {
			error(e);
		}
	}

}
