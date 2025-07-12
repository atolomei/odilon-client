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

import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;

public class ExecutorMetadata extends Executor {
			
	static public Logger logger = Logger.getLogger(ExecutorMetadata.class.getName());
	
	private Bucket bucket;
	
	public ExecutorMetadata(long sleepTime, OdilonClient client, Bucket bucket) {
		super(sleepTime, client, bucket);
	}

	public void init() {
		try {
			this.bucket=getClient().getBucket(getTestBucket().getName());
		} catch (ODClientException e) {
			 error(e);
		}
	}

	@Override
	public void executeTask() {
	
		try {
			
			ResultSet<Item<ObjectMetadata>> rs=getClient().listObjects(bucket);
			Item<ObjectMetadata> item;
			
			int counter = 0;
			
			while (rs.hasNext() && counter++<5) {
				item = rs.next();
				if (item.isOk())
					logger.debug( item.getObject().toString());
				else 
					logger.error(item.getErrorString());
			}
			logger.debug( this.getClass().getSimpleName() + " -> listObjects ok");
			
		} catch (ODClientException e) {
			 error(e);
		}
	}

}
