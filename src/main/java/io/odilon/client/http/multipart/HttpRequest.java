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
package io.odilon.client.http.multipart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;

import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.net.ErrorCode;
import io.odilon.net.ODHttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Base64;

/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
@JsonInclude(Include.NON_NULL)
public class HttpRequest {
		
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(HttpRequest.class.getName());

	private static final int BUFFER = 4096;
	private static final int CHUNK_LEN = 4096;

	private static final int CONNECTION_TIMEOUT   = ODClient.DEFAULT_CONNECTION_TIMEOUT;
	private static final String DEFAULT_USER_AGENT = ODClient.DEFAULT_USER_AGENT; 

	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	static {
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	private int chunk = 0;
	private HttpURLConnection conn = null;
	private String url;
	private String credentials;
	private String apiToken;
	private PrintWriter writer;
	private BufferedReader reader;
	private ProgressListener listener;
	protected String LINE_FEED = "\r\n";

    public HttpRequest(String url, String credentials) {
        setUrl(url);
        setCredentials(credentials);
    }

    public HttpRequest(String url, String credentials, ProgressListener listener) {
        setUrl(url);
        setCredentials(credentials);
        setListener(listener);
    }

    public ProgressListener getListener() {
        return listener;
    }

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }
 
    public int getChunk() {
		return chunk;
	}

	public void setChunk(int chunk) {
		this.chunk = chunk;
	}

	/**
	 * 
	 * @param <T>
	 * @param requestEntity
	 * @param responseType
	 * @return
	 * @throws ODClientException
	 */
    public <T> T exchange(HttpEntity requestEntity, TypeReference<T> responseType) throws ODClientException {
    
    	int responseCode = 0;
        try {
        	try {
				write(requestEntity);
			} catch (IOException e1) {
				throw new InternalCriticalException(e1);
			}

        	
        	try {
				responseCode = getResponseCode();
			} catch (IOException e1) {
				throw new InternalCriticalException(e1);
			}
            

        	if (responseCode == HttpURLConnection.HTTP_OK) {
            	T response;
				try {
					response = (T)mapper.readValue(getResponse(), responseType);
				} catch (IOException e) {
					throw new InternalCriticalException(e);
				}
            	return response;
            }
            else {
            
    			if (responseCode==ODHttpStatus.UNAUTHORIZED.value()) {
    				   throw new ODClientException(ODHttpStatus.UNAUTHORIZED.value(), 
    											   ErrorCode.ACCESS_DENIED.value(), 
    											   ErrorCode.ACCESS_DENIED.getMessage()
    								);
    			}
				StringBuilder  rm = new StringBuilder();
				try {
					rm.append(" url -> " + getUrl());
					BufferedReader r=getErrorReader();
					if (r!=null) {
						String s = r.readLine();
						if (s!=null)
							rm.append(" | " + s);
					}
					throw new ODClientException(responseCode, 
							   ErrorCode.INTERNAL_ERROR.getCode(), 
							   rm.toString());
					
				} catch (IOException e) {
					throw new InternalCriticalException(e);
				}
 			}
        }
        finally {
       		close();
        }
    }


    public PrintWriter getWriter( String charset) throws IOException  {
        if (writer==null) {
            OutputStream outputStream = getConnection().getOutputStream();
            //writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset ), true);
        }
        return writer;
    }

    public BufferedReader getReader() throws IOException {
        if (reader==null) {
                reader = new BufferedReader(new InputStreamReader(getConnection().getInputStream()));
        }
        return reader;
    }

    public BufferedReader getErrorReader() throws IOException {
        BufferedReader reader = null;
        if (reader==null) {
              reader = new BufferedReader(new InputStreamReader(getConnection().getErrorStream()));
        }
        return reader;
    }


    public int getResponseCode() throws IOException {
        return getConnection().getResponseCode();
    }

    public HttpURLConnection getConnection() throws IOException {
        if (conn == null)
            setConnection(openConnection());
        return conn;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public URL getUrl() throws MalformedURLException {
        return new URL(this.url);
    }

    public String getCredentials() {
        return credentials;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setConnection(HttpURLConnection connection) {
        this.conn = connection;
    }

    protected void write(HttpEntity entity) throws IOException {
        
        OutputStream outputStream = getConnection().getOutputStream();
        byte[] buffer = new byte[BUFFER];
        int bytesRead = -1;
        long bytesWritten = 0;
        long totalBytes = entity.getSize();
        int progress = 0;
        try  (InputStream inputStream = entity.getStream()) {
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            outputStream.write( buffer, 0, bytesRead);
	            bytesWritten += bytesRead;
	            progress = totalBytes>0 ? (int)((double) bytesWritten/(double)totalBytes * 100) : 0;
	            if (getListener()!=null) getListener().onUpdate(progress);
	        }
	        outputStream.flush();
        } 
        
        //logger.debug("Written -> " + String.valueOf(bytesWritten) + " bytes");
    }

    
    protected String getResponse() throws  IOException {
        String response = null, line;
        while ((line = getReader().readLine()) != null) {
            response = response == null ? line : response + LINE_FEED + line;
        }
        return response;
    }

   
    
    
    protected String getErrorResponse() throws  IOException {
        String response = null, line;
        while ((line = getErrorReader().readLine()) != null) {
            response = response == null ? line : response + LINE_FEED + line;
        }
        return response;
    }

    protected String getRequestMethod() {
        return "GET";
    }

    protected String getContentType() {
        return "application/json";
    }

    protected boolean getDoOutput() {
        return false;
    }
    
    
    protected HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) getUrl().openConnection();
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(getDoOutput());
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setRequestMethod(getRequestMethod());
        conn.setRequestProperty("Content-Type", getContentType());
        conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
        conn.setChunkedStreamingMode(CHUNK_LEN);

        if (getChunk()>0)
        	conn.setChunkedStreamingMode(getChunk());
        
        String base64Credentials = Base64.getEncoder().encodeToString(getCredentials().getBytes());
        
        if (getApiToken()!=null)
            conn.setRequestProperty("Authorization", "Bearer " + getApiToken());
        else
            conn.setRequestProperty("Authorization", "Basic " + base64Credentials);
        return conn;
    }

    protected void close() {
        conn = null;
    }
}




