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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.RedundancyLevel;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;
import io.odilon.util.OdilonFileUtils;

/**
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class TextRunIntegrityCheck extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());

	long LAPSE_BETWEEN_PUT_MILLISECONDS = 1600;


	private OffsetDateTime showStatus = OffsetDateTime.now();
	
	public TextRunIntegrityCheck() {
	}

	@Override
	public void executeTest() {

		preCondition();

		try {
			 
			
			String s=((ODClient) getClient()).checkIntegrity(true);
			getMap().put("check", s);
			
			showResults();
		} finally {
	
		}
	}

		public boolean preCondition() {
			return true;
		}

	
}
