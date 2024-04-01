package io.odilon.test.base;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.odilon.log.Logger;
import io.odilon.model.ObjectMetadata;
import io.odilon.util.ODFileUtils;

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

		if (!files.containsKey(iVersion))
			throw new IllegalArgumentException("invalid version");
				
		if (!sha256.containsKey(iVersion)) {
			try {
				String str = ODFileUtils.calculateSHA256String( files.get(iVersion));
				sha256.put(iVersion, str);
			} catch (NoSuchAlgorithmException | IOException e) {
				logger.error(e);
			}
			
		}
		return sha256.get(iVersion);
		
	}


	
}
