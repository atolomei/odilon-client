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
import io.odilon.json.OdilonObjectMapper;
import io.odilon.log.Logger;
import io.odilon.net.ErrorCode;
import io.odilon.net.ODHttpStatus;

//import tools.jackson.core.type.TypeReference;
//import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 
 * @author aferraria@novamens.com (Alejo Ferraria)
 * 
 * 
 * 
 * 
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
 
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;






@JsonInclude(Include.NON_NULL)
public class HttpRequest {
		
	 
	private static final Logger logger = Logger.getLogger(HttpRequest.class.getName());

	 
	private static final int DEFAULT_CHUNK_LEN = 16 * 1024;

	private static final int CONNECTION_TIMEOUT   = ODClient.DEFAULT_CONNECTION_TIMEOUT;
	private static final String DEFAULT_USER_AGENT = ODClient.DEFAULT_USER_AGENT; 

 
	
	private static final  OdilonObjectMapper mapper = new OdilonObjectMapper();
	
	private int chunk = 0;
	private HttpURLConnection conn = null;
	private String url;
	private String credentials;
	private String apiToken;
	private PrintWriter writer;
	private BufferedReader reader;
	private ProgressListener listener;
	

	private final boolean isSSL;
	private final boolean isAcceptAllCertificates;
	
	protected String LINE_FEED = "\r\n";
	
	
    public HttpRequest(String url, String credentials, boolean isSSL, boolean isAcceptAllCertificates) {
    	this.isSSL=isSSL;
    	this.isAcceptAllCertificates=isAcceptAllCertificates;
    	setUrl(url);
        setCredentials(credentials);
    }

  	
    public HttpRequest(String url, String credentials, boolean isSSL, boolean isAcceptAllCertificates, ProgressListener listener) {
    	this.isSSL=isSSL;
    	this.isAcceptAllCertificates=isAcceptAllCertificates;
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

	public boolean isSSL() {
  		return isSSL;
  	}

  	public boolean isAcceptAllCertificates() {
		return isAcceptAllCertificates;
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
        if (this.writer==null) {
            OutputStream outputStream = getConnection().getOutputStream();
            this.writer = new PrintWriter(new OutputStreamWriter(outputStream, charset ), true);
        }
        return this.writer;
    }

    public BufferedReader getReader() throws IOException {
        if (this.reader==null)
        	this.reader = new BufferedReader(new InputStreamReader(getConnection().getInputStream()));
        return this.reader;
    }

    public BufferedReader getErrorReader() throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(getConnection().getErrorStream()));
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
        return this.credentials;
    }

    public String getApiToken() {
        return this.apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setConnection(HttpURLConnection connection) {
        this.conn = connection;
    }

    protected void write(HttpEntity entity) throws IOException {
        
        OutputStream outputStream = getConnection().getOutputStream();
        int buffSize = getChunk()>0?getChunk() : DEFAULT_CHUNK_LEN;
        
        buffSize = 1024;
        
        byte[] buffer = new byte[ buffSize ];
        int bytesRead = -1;
        long bytesWritten = 0;
        long totalBytes = entity.getSize();
        int progress = 0;
        
        logger.debug("buffSize -> " + buffSize);
        
        int buffersRead = 0;
        try  (InputStream inputStream = entity.getStream()) {
        	
        	while ((bytesRead = inputStream.read(buffer)) != -1) {
	            outputStream.write( buffer, 0, bytesRead);
	            bytesWritten += bytesRead;
	            buffersRead++;
	            progress = totalBytes>0 ? (int)((double) bytesWritten/(double)totalBytes * 100) : 0;
	            if (getListener()!=null) getListener().onUpdate(progress);
	        }
	        outputStream.flush();
        }
        catch (Exception e) {
        	logger.debug(e);
        	logger.debug("error buffers read -> " + buffersRead);
        }
        finally {
            //logger.debug("Written -> " + String.valueOf(bytesWritten/1000) + " KBytes");
        }
        
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
    	

    	if (isSSL() && isAcceptAllCertificates()) {
	        try {
		        	TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			                    return null;
			                }
			                public void checkClientTrusted(X509Certificate[] certs, String authType) {
			                }
			                public void checkServerTrusted(X509Certificate[] certs, String authType) {
			                }
		            	}
		        	};
		
		        // Install the all-trusting trust manager
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
		        // Create all-trusting host name verifier
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            public boolean verify(String hostname, SSLSession session) {
		                return true;
		            }
		        };
		        
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		        
	        } catch (Exception e) {
	        		throw new IOException(e);
	        }
    	}
    	
    	HttpURLConnection conn = (HttpURLConnection) getUrl().openConnection();

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(getDoOutput());
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setRequestMethod(getRequestMethod());
        conn.setRequestProperty("Content-Type", getContentType());
        conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

        if (getChunk()>0)
        	conn.setChunkedStreamingMode(getChunk());
        else
            conn.setChunkedStreamingMode(DEFAULT_CHUNK_LEN);

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




