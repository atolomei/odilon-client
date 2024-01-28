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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import io.odilon.log.Logger;


public class TempCache {
			
		private static final Logger logger = Logger.getLogger(TempCache.class.getName());

		private static final int BUFFER_SIZE = 4096;
		
		static private final String cacheSubdir  = "tmp";	  
		static private final String linux_home   = (new File(System.getProperty("user.dir"))).getPath();
		static private final String windows_home = System.getProperty("user.dir");
		
		private AtomicBoolean isTempdir =  new AtomicBoolean(false);
	  
		public TempCache() {
		}
		
		public synchronized void removeCache(String fileName) {
			  File file = new File(getTempDir() + File.separator + fileName);
		 		synchronized (this) {
					  if (file.exists())
						FileUtils.deleteQuietly(file);
		 		}
		  }
		  
		  /*
		   * sync is not needed because the filename is unique
		   * 
		   * @param fileName
		   * @param stream
		   * @return
		   */
		public synchronized File addCache(String fileName, InputStream stream) {
					String nf = getTempDir() + File.separator + fileName;
			 		byte[] buf = new byte[BUFFER_SIZE];
			 		int bytesRead;
					BufferedOutputStream out = null;
					try {
						 out = new BufferedOutputStream(new FileOutputStream(nf), BUFFER_SIZE);
						 while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0)
							  out.write(buf, 0, bytesRead);
						 return new File(nf);
						 
					} catch (IOException e) {
						logger.error(e);
						throw new RuntimeException(e);		
						
					} finally {
						
						if (stream!=null) { 
							try {
								stream.close();
							} catch (IOException e) {
								logger.error(e);
							}	
						}
						
						if (out!=null) { 
							try {
								out.close();
							} catch (IOException e) {
								logger.error(e);
							}	
						}
					}
				}
		
		
  private String getCacheWorkDir() {									
		return getHomeDirAbsolutePath() + File.separator + cacheSubdir;
  }

		
	private String getHomeDirAbsolutePath() {
		if (isLinux())
			return linux_home;
		return windows_home;
	}
		
	  
	private String getTempDir() {
			  if (this.isTempdir.get()) {
		 			return getCacheWorkDir();
			  }
		 		synchronized (this) {
			 		String xdir = getCacheWorkDir();
			 		File base = new File(xdir);
			 		if (!base.exists()) {
			 			synchronized (this) {
				 			try {
								FileUtils.forceMkdir(base);					 
							} catch (IOException e) {
								logger.error(e);
							}
			 			}
			 		}
			 		else if (!base.isDirectory()) {
			 				FileUtils.deleteQuietly(base);
				 			try {
				 				FileUtils.forceMkdir(base);
							} catch (IOException e) {
								logger.error(e);
							}
			 		}
			 		this.isTempdir.set(true);
			 		return xdir;
		 		}
		  }

	private static boolean isLinux() {
		if  (System.getenv("OS")!=null && System.getenv("OS").toLowerCase().contains("windows")) 
			return false;
		return true;
	}

}
