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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SharedConstant;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;

/**
 * <p>
 * A presigned URL is a way to grant temporary access to an Object, for example
 * in an HTML webpage. It remains valid for a limited period of time which is
 * specified when the URL is generated.
 * </p>
 *
 */
public class TestPresignedUrl extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestPresignedUrl.class.getName());

	private Bucket bucket_1;
	private String bucketName = null;

	public TestPresignedUrl() {
	}

	@Override
	public void executeTest() {

		try {

			if (getClient().listBuckets().isEmpty()) {
				createBucket();
				addFiles();
			}

			org.junit.Assert.assertFalse("must have at least 1 bucket", getClient().listBuckets().isEmpty());

			this.bucket_1 = null;

			for (Bucket bu : getClient().listBuckets()) {
				logger.debug(bu.toString());
				if (!getClient().isEmpty(bu.getName())) {
					this.bucket_1 = bu;
					break;
				}
			}

			org.junit.Assert.assertFalse("bucket must not be empty",
					this.bucket_1 == null || (getClient().isEmpty(this.bucket_1.getName())));

			{
				
				logger.debug("60 seconds expire time");

				List<String> list = new ArrayList<String>();
				
				ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
				int counter = 0;
				int total = 0;
				while (rs.hasNext() && counter++ < getMax()) {
					Item<ObjectMetadata> item = rs.next();
					if (item.isOk()) {
						ObjectMetadata meta = item.getObject();
					
						String str = getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName, Optional.of(60));
						list.add(str);
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> " + str);
						total++;
					}
				}

				/**
				try {					
					Thread.sleep(60000);
				} catch (InterruptedException e) {
				}
				
				list.forEach( item -> {
						logger.debug( item + "  | valid -> " + getClient().isValidPresignedUrl( item ) );
				});
				**/
				getMap().put("presigned test (60 seconds) -> " + String.valueOf(total), "ok");
			}

			logger.debug("");
			logger.debug("");
			
			{
				
				logger.debug("14 days expire time");

				
				ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket_1.getName());
				int counter = 0;
				int total = 0;
				while (rs.hasNext() && counter++ < getMax()) {
					Item<ObjectMetadata> item = rs.next();
					if (item.isOk()) {
						ObjectMetadata meta = item.getObject();
						logger.debug(meta.bucketName + " / " + meta.objectName + " -> "
								+ getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName));
						total++;
					}
				}
				getMap().put("presigned test (default value: " + String.valueOf(SharedConstant.DEFAULT_EXPIRY_TIME/3600)  + " hours ) -> " + String.valueOf(total), "ok");
			}
			
			logger.debug("");

			showResults();

		} catch (ODClientException e) {
			error(e);
		}
	}

	private void createBucket() {

		bucketName = randomString(10);

		try {
			getClient().createBucket(bucketName);
		} catch (ODClientException e) {
			error(e, bucketName);
		}
	}

	/**
	 * @return
	 */
	private boolean addFiles() {

		File dir = new File(getSourceDir());

		if ((!dir.exists()) || (!dir.isDirectory())) {
			throw new RuntimeException("Dir not exists or the File is not Dir -> " + getSourceDir());
		}

		int counter = 0;

		String bucketName = null;
		bucketName = this.bucket_1.getName();

		// put files
		//
		for (File fi : dir.listFiles()) {

			if (counter >= getMax())
				break;

			if (isElegible(fi)) {
				String objectName = FSUtil.getBaseName(fi.getName()) + "-"
						+ String.valueOf(Double.valueOf((Math.abs(Math.random() * 100000))).intValue());
				try {
					getClient().putObject(bucketName, objectName, fi);
					counter++;
				} catch (ODClientException e) {
					error(String.valueOf(e.getHttpStatus()) + " " + e.getMessage() + " "
							+ String.valueOf(e.getErrorCode()) + " | " + bucketName + "/" + objectName);
				}
			}
		}

		return true;

	}

}
