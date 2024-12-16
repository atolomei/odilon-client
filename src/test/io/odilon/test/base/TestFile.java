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
package io.odilon.test.base;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import io.odilon.log.Logger;
import io.odilon.model.ObjectMetadata;
import io.odilon.util.OdilonFileUtils;


/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class TestFile {

	private static Logger logger = Logger.getLogger(TestFile.class.getName());

	public String bucketName;
	public String objectName;
	
	private Map<Integer,File> files  = new HashMap<Integer, File>();
	private Map<Integer, String> sha256 = new HashMap<Integer, String>(); // sha of source files
	private Map<Integer, ObjectMetadata> metadata  = new HashMap<Integer, ObjectMetadata>();

	public  TestFile(File file, String bucketName, String objectName) {
		this.bucketName = bucketName;
		this.objectName =objectName;
		addSrcFileVersion(file);
	}
	
	
	/**
	public  TestFile(File file, String bucketName, String objectName, ObjectMetadata meta) {
		
		this.bucketName = bucketName;
		this.objectName =objectName;
		meta.put(meta.version);
		
	}
	**/

				
	public void addMetaVersion(ObjectMetadata meta) {
		metadata.put(Integer.valueOf(meta.version), meta);
	}


	public void addSrcFileVersion(File file) {
		int newVersion = files.size();
		files.put(Integer.valueOf(newVersion), file);
	}

	public File getSrcFile(int version) {
		return files.get(version);
	}

			
	public ObjectMetadata getMetadata(int version) {
		return metadata.get(version);
	}

	
	public String getSrcFileSha256(int version) {
	
		Integer iVersion = Integer.valueOf(version);

		if (!files.containsKey(iVersion)) {
		    logger.error("invalid version -> " + String.valueOf(version));
		    throw new IllegalArgumentException("invalid version -> " + String.valueOf(version));
		}
				
		if (!sha256.containsKey(iVersion)) {
			try {
				String str = OdilonFileUtils.calculateSHA256String( files.get(iVersion));
				sha256.put(iVersion, str);
			} catch (NoSuchAlgorithmException | IOException e) {
			    logger.error(e.getClass().getName() + " | version -> " + String.valueOf(version));
			    logger.error(e);
			}
			
		}
		return sha256.get(iVersion);
		
	}


	
}
