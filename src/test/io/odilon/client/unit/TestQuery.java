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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

import io.odilon.client.OdilonClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.test.base.BaseTest;

public class TestQuery extends BaseTest {

	private static final Logger logger = Logger.getLogger(TestObjectPutGet.class.getName());

	private Bucket bucket_1 = null;
	private Map<String, String> map = new HashMap<String, String>();

	public TestQuery() {
	}
	
	
	@Test
	@Override
	public void executeTest() {
		
		preCondition();
		
		loadTestBucket();
		listObjects();
		listPageSize();
		listPrefix();
		emptyTestBucket();
		
		logger.debug("--------------------");
		map.forEach((k,v) -> logger.debug(k+" -> "+ v));
		logger.debug("--------------------");
		logger.debug("done");
	}

	
	

	public void emptyTestBucket() {
	
		try {
			
			List<ObjectMetadata> list = new ArrayList<ObjectMetadata>();
			
			ResultSet<Item<ObjectMetadata>>  rs=getClient().listObjects("test-query");
			
			while (rs.hasNext()) {
				Item<ObjectMetadata> item =rs.next();
				if (item.isOk()) {
					list.add(item.getObject());
				}
			}
			

			list.forEach(o -> {
				try {
					getClient().deleteObject(o.bucketName, o.objectName);
				} catch (ODClientException e) {
					error(e);
				}
			});
			
			
		} catch (ODClientException e) {
			error(e);
		}
		
	}
	
	
	public void loadTestBucket() {
		putObjects("test-query");
	}
	
	
	
	public boolean preCondition() {
		try {
			if (!getClient().existsBucket("test-query")) {
					getClient().createBucket("test-query");
			}
			bucket_1 = getClient().getBucket("test-query");
			
		} catch (ODClientException e) {
			error(e);
		}
		return true;
	}
	/**
	 * 
	 */
	public void listPrefix() {
		

		try {

			ResultSet<Item<ObjectMetadata>> resultSet = getClient().listObjects(bucket_1.getName(), Optional.of("e"), Optional.of(10));
	    	
	    	if (resultSet == null)
	    		error("resultSet is null");
	    	
	    	int counter = 0;
	    	
	    	Map<String,ObjectMetadata> map_1 = new HashMap<String,ObjectMetadata>();
	    	
	    	while (resultSet.hasNext() && counter<100) {
	    		Item<ObjectMetadata> item = resultSet.next();
	    		if (item.isOk()) {
	    			logger.debug(String.valueOf(counter+1) + " -> " + item.getObject().objectName);
	    			map_1.put(item.getObject().bucketName + "-" + item.getObject().objectName, item.getObject());
	    		}
	    		else
	    			logger.debug(item.getErrorString());
	    		counter++;
	    	}
	     	logger.debug("listPrefix ok");
	        map.put("listObjects prefix", "ok");
	    
	    } catch (ODClientException e) {
	    	error(e);
		}
	}

	
	public void listPageSize() {
		
		logger.debug("listPageSize 5");
		try {
			
	    	ResultSet<Item<ObjectMetadata>> resultSet = getClient().listObjects(bucket_1.getName(), Optional.empty(), Optional.of(5));
	    	if (resultSet == null)
	    		throw new RuntimeException("resultSet is null");
	    	int counter = 0;
	    	Map<String,ObjectMetadata> map_1 = new HashMap<String,ObjectMetadata>();
	    	
	    	while (resultSet.hasNext() && counter<100) {
	    	
	    		Item<ObjectMetadata> item = resultSet.next();
	    		
	    		if (item.isOk()) {
	    			logger.debug(String.valueOf(counter+1) + " -> " + item.getObject().objectName);
	    			map_1.put(item.getObject().bucketName + "-" + item.getObject().objectName, item.getObject());
	    		}
	    		else
	    			logger.debug(item.getErrorString());
	    		counter++;
	    	}
	    	
	    	logger.debug("listPageSize ok");
	        map.put("listPageSize", "ok");
	    
	    } catch (ODClientException e) {
	    		error(e);
		}
	}
	
	
		
	/**
	 * 
	 * 
	 */
	public void listObjects() {
		
		logger.debug("list all defaults");

		try {
			
	    	ResultSet<Item<ObjectMetadata>> resultSet = getClient().listObjects(bucket_1.getName(), Optional.empty(), Optional.empty());
	    	
	    	if (resultSet == null)
	    		throw new RuntimeException("resultSet is null");
	    	
	    	int counter = 0;
	    	
	    	Map<String,ObjectMetadata> map_1 = new HashMap<String,ObjectMetadata>();
	    	
	    	while (resultSet.hasNext() && counter<100) {
	    		Item<ObjectMetadata> item = resultSet.next();
	    		if (item.isOk()) {
	    			logger.debug(String.valueOf(counter) + " -> " + item.getObject().objectName);
	    			map_1.put(item.getObject().bucketName + "-" + item.getObject().objectName, item.getObject());
	    		}
	    		else
	    			logger.debug(item.getErrorString());
	    		counter++;
	    	}
	    	
	    	logger.debug("listObjects ok");
	    	map.put("listObjects", "ok");
	    
	    } catch (ODClientException e) {
	    		error(e);
		}
	}
}





























/** -----------------------------------------------------------
 * 
 * 

private void dummy() {

Path start = Paths.get(TEMP_DIR);

Path medio= Paths.get("c:\\temp\\2023-03-23-entregado-ba.pdf");

Stream<Path> stream = null;

try {

	 stream = Files.walk(start, 0);
    //.sorted()
	 
	int counter = 0;
	
	
	Iterator<Path> it = stream
			.skip(3)
           .filter(file -> !Files.isDirectory(file))
           .filter(file -> checkFileSize(file)).iterator();

	int MAX = 5;
	List<Path> list = new ArrayList<Path>();
	while (it.hasNext() && counter++ < MAX) {
		list.add(it.next());
	}
	
	//.collect(Collectors.toList());
	
	int n = 0;
	//list.forEach( item -> logger.debug(item));



logger.debug("done");

} catch (IOException e) {
	logger.error(e);
}
finally {
	if (stream!=null)
		stream.close();
}
}
	    	/**List<ObjectMetadata> list = getClient().listAllObjectMetadata(bucket_1.getName());
	    	Map<String,ObjectMetadata> map_2 = new HashMap<String,ObjectMetadata>();
	    	list.forEach( item -> map_2.put(item.bucketName + "-" + item.objectName, item));
	    	map_1.forEach( (k,v) -> 
	    							{
	    									if (!map_2.containsKey(k))
	    										logger.debug("error map_2 does not have -> " + k);
	    											
	    							});
	    	map_2.forEach( (k,v) -> 
			{
					if (!map_1.containsKey(k))
						logger.debug("error map_1 does not have -> " + k);
							
			});

	    	logger.debug("---------------------------------------------------------------------");
	    	logger.debug("done ok");
			Assert.assertNotNull(list);
			*/
