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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;
import io.odilon.test.base.TestFile;

public class TestGetObjects extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(TestGetObjects.class.getName());
	
	static final String TEMP_DIR = "c:"+File.separator+"temp";
	static final String DOWNLOAD_DIR = "c:"+File.separator+"temp" + File.separator+"download";
	
	static final int BUFFER_SIZE = 4096;
	
	
	
	private Bucket bucket_1 = null;
	private Map<String, TestFile> testFiles = new HashMap<String, TestFile>();
	
	private final File saveDir = new File(DOWNLOAD_DIR);
	
	public TestGetObjects () {
	}
	
	

	
	@Override
	public void executeTest() {

		preCondition();
		
		if (!testGetObjects())
			error("testGetObjects");
		
		showResults();
	    
	}

	
	private boolean testGetObjects() {
		
		try {
			int counter = 0;
			
			ResultSet<Item<ObjectMetadata>> resultSet;
			
			try {
				resultSet = getClient().listObjects(bucket_1.getName(), Optional.empty(), Optional.empty());
				
				while (resultSet.hasNext() && counter++< getMax()) {
					
					Item<ObjectMetadata> item = resultSet.next();
		    		if (item.isOk()) {
		    			logger.debug(String.valueOf(counter) + " -> " + item.getObject().objectName);
		    			ObjectMetadata meta = item.getObject();
		    			try {
							
		    				getClient().getObject(meta.bucketName, meta.objectName, DOWNLOAD_DIR + File.separator + meta.fileName);
		    				
						} catch (IOException e) {
							error(e);
						}
		    		}
		    		else
		    			logger.debug(item.getErrorString());
				}
				
			} catch (ODClientException e) {
				error(e);
			}
			
			getMap().put("testGetObjects", "ok");
			return true;
				
        }
		finally {
				//try {
				//	FileUtils.forceDelete(saveDir);
				//} catch (IOException e) {
				//	logger.error(e);
				//	error(e.getClass().getName() + " | " + e.getMessage());
				//}
			}
	}

	public boolean preCondition() {

        File dir = new File(TEMP_DIR);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +TEMP_DIR);
		}

        try {
			
        	String p=ping();
			
			if ((p==null) || (!p.equals("ok")))
				error("ping  -> " + ((p!=null) ? p : "null"));
			else {
				getMap().put("ping", "ok");
			}
		} catch (Exception e)
		{
			error(e.getClass().getName() + " | " + e.getMessage());
		}
        
        
        File tmpdir = new File(DOWNLOAD_DIR);
        
        if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
        	try {
				FileUtils.forceDelete(tmpdir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(tmpdir);
				
		} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
		}
		
        
        
		try {	
			if (getClient().listBuckets().isEmpty())
				error("listBuckets().isEmpty()");
			
			bucket_1 = getClient().listBuckets().get(0);
			
		} catch (ODClientException e) {
			error(e.getClass().getName() + " | " + e.getMessage());
		}
		
		return true;
	}

}
