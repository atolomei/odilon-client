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
package io.odilon.client.util;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

/**
 * <p>General utilities for files</p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class FSUtil {

	
	static public String getBaseName(String filename) {
		return FilenameUtils.getBaseName(filename);
	}
	
	
	static public boolean isImage(File file) {
		if (file.exists() && !file.isDirectory())
			return isImage(file.getName());
		return false;
	}

							
	static public boolean isVideo(File file) {
		if (file.exists() && !file.isDirectory())
			return isVideo(file.getName());
		return false;
	}
 	
							
	static public boolean isAudio(File file) {
		if (file.exists() && !file.isDirectory())
			return isAudio(file.getName());
		return false;
	}
	
	/*
	 * 
	 * all but webp
	 * @param string
	 * @return
	 */
	static public boolean isGeneralImage(String string) {
		return string.toLowerCase().matches("^.*\\.(png|jpg|jpeg|gif|bmp|heic)$"); 
	}
	
	static public boolean isImage(String string) {
		return isGeneralImage(string) || string.toLowerCase().matches("^.*\\.(webp)$"); 
	}
	
 	
	static public boolean isText(File file) {
		if (file!=null)
			return isText(file.getName());
		return false;
	}
	
 	static public boolean isText(String name) {
		return name.toLowerCase().matches("^.*\\.(c|cpp|json|js|net|bat|sh|ini|text|psql|plsql|java|properties|txt|xml|html|css|sql|log|err|lst|asc|me|eml|odt|tab|tex|bib|utf8|sxg|wp5|wp6|wp7|faq)$"); 
	}
 	
 	
 	static public boolean isWord(File file) {
		if (file!=null)
			return isWord(file.getName());
		return false;
	}
 	
 	static public boolean isWord(String name) {
		return name.toLowerCase().matches("^.*\\.(doc|docx|rtf)$"); 
	}
 	
 	static public boolean isPdf(String filename) {
		return filename.toLowerCase().matches("^.*\\.(pdf)$"); 
	}

 	static public boolean isPdf(File file) {
		return file.getName().toLowerCase().matches("^.*\\.(pdf)$"); 
	}

 	static public boolean isExcel(File file) {
		if (file!=null)
			return isExcel(file.getName());
		return false;
	}
 	
 	static public boolean isExcel(String name) {
		return name.toLowerCase().matches("^.*\\.(xls|xlsx|xlsm)$"); 
	}

 										
 	static public boolean isMSOffice(String name) {
		return  isWord(name)|| isExcel(name) || isPowerpoint(name); 
	}
 	
	static public boolean isCSV(File file) {
		if (file==null)
			return false;
		return file.getName().toLowerCase().matches("^.*\\.(csv)$"); 
	}
 	
 	static public boolean isCSV(String name) {
		if (name==null)
			return false;
		return name.toLowerCase().matches("^.*\\.(csv)$"); 
	}
 	
 	
 	static public boolean isPowerpoint(File file) {
		if (file!=null)
			return isPowerpoint(file.getName());
		return false;
	}

 	static public boolean isPowerpoint(String name) {
 		return name.toLowerCase().matches("^.*\\.(ppt|pptx)$"); 
	}
 	
 	
 	static public boolean isJar(String filename) {
		return (filename.toLowerCase().matches("^.*\\.(jar|war)$") ); 
	}
 	
	static public boolean isZip(String filename) {
		return (filename.toLowerCase().matches("^.*\\.(zip|gz|gzip|rar|bz2|lz|lzma|lzo|rz|z|arc|arj|zz|tar|par)$") ); 
	}

    static public boolean isVideo(String filename) {
		return (filename.toLowerCase().matches("^.*\\.(mp4|flv|aac|ogg|wmv|3gp|avi|swf|svi|wtv|fla|mpeg|mpg|mov|m4v)$") ); 
	}

    static public boolean isAudio(String filename) {
		return filename.toLowerCase().matches("^.*\\.(mp3|wav|ogga|ogg|aac|m4a|m4a|aif|wma)$"); 
	}
    
    static public boolean isExecutable(File srcfile) {
		return srcfile.getName().matches("^.*\\.(exe|EXE)$"); 
	}
    
    static public boolean isExecutable(String fileName) {
    	return fileName.matches("^.*\\.(exe|EXE)$"); 
	}
    
    static public boolean isMsg(File srcfile) {
		return srcfile.getName().matches("^.*\\.(msg|MSG)$"); 
	}
    
 	static public boolean isMSOffice(File file) {
		return (isWord(file) ||
			   isExcel(file) ||
			   isPowerpoint(file));
  	}

    static public boolean isOCRCandidate(String filename) {
		if (filename.matches("^.*\\.(png|PNG|jpg|JPG|gif|webp|WEBP|GIF|pdf|PDF|tif|tiff|TIF|TIFF)$") ) 
			return true;
		else
			return false;
	}
    
	static public boolean isOCRCandidate(File file) {
		return isOCRCandidate(file.getName());
	}

	
    
 	static public String getExtension(String filename) {
		return FilenameUtils.getExtension(filename);
	}
	
	static public String getEncrytedFileName(String filename) {
		return filename+".aes";
	}

	public static boolean isLog(String name) {
		return name.toLowerCase().matches("^.*\\.(log|lg)$");
	}

}
