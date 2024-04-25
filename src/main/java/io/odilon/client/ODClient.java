/*
 * Odilon Java SDK 
 * (C) 2023 Novamens 
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
package io.odilon.client;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.odilon.client.error.ODClientException;
import io.odilon.client.http.HeaderParser;
import io.odilon.client.http.HttpRequestBody;
import io.odilon.client.http.HttpResponse;
import io.odilon.client.http.Method;
import io.odilon.client.http.ResponseHeader;
import io.odilon.client.http.Scheme;
import io.odilon.client.http.multipart.HttpFileEntity;
import io.odilon.client.http.multipart.HttpMultipart;
import io.odilon.client.util.Digest;
import io.odilon.client.util.FSUtil;
import io.odilon.client.util.InetAddressValidator;

import io.odilon.errors.InternalCriticalException;
import io.odilon.errors.OdilonErrorProxy;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SharedConstant;
import io.odilon.model.SystemInfo;
import io.odilon.model.list.Item;
import io.odilon.model.list.CachedDataProvider;
import io.odilon.model.list.DataList;
import io.odilon.model.list.ResultSet;
import io.odilon.net.ErrorCode;
import io.odilon.net.ODHttpStatus;
import io.odilon.util.Check;
import io.odilon.util.RandomIDGenerator;
import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * <p>This class implements the Interface {@link OdilonClient}, Odilon Object Storage client.</p>
 * <p>For examples on using this library, please visit <a href="http://odilon.io">http://odilon.io</a></p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * @author aferraria@novamens.com (Alejo Ferraria)
 * 
 */
public class ODClient implements OdilonClient {

	private static final io.odilon.log.Logger logger = Logger.getLogger(ODClient.class.getName());

	private static final int BUFFER_SIZE = 8192;
									
	private static final String API_SERVICE_REQUES_ADD						[] = {"servicerequest", "add"};
	
	/** 
	 * 	MONITOR  
	 */
	private static final String API_PING 									[] = {"ping"};
	private static final String API_METRICS									[] = {"metrics"};
	private static final String API_SYSTEM_INFO								[] = {"systeminfo"};
	
	/** 
	 * 	BUCKET 
	 */
	private static final String API_BUCKET_LIST 							[] = {"bucket", "list"};
	private static final String API_BUCKET_GET	 							[] = {"bucket", "get"};
	private static final String API_BUCKET_EXISTS 							[] = {"bucket", "exists"};
	private static final String API_BUCKET_CREATE	 						[] = {"bucket", "create"};
	//private static final String API_BUCKET_RENAME	 						[] = {"bucket", "rename"};
	private static final String API_BUCKET_DELETE	 						[] = {"bucket", "delete"};
	private static final String API_BUCKET_ISEMPTY							[] = {"bucket", "isempty"};
	private static final String API_BUCKET_LIST_OBJECTS						[] = {"bucket", "objects"};
								
	private static final String API_BUCKET_DELETE_ALL_PREVIOUS_VERSION		[] = {"bucket", "deleteallpreviousversion"};
	
	
	/** 
	 * OBJECT 
	 */
	
	private static final String API_OBJECT_GET 								[] = {"object", "get"};
	
	/** get Inpustream of the version passed as parameter, 
	 * null if head is version 0 or parameter is non existent, 
	 * or version were wiped  */
	private static final String API_OBJECT_GET_VERSION 						[] = {"object", "getversion"};
	
	/** get File of the version previous to head, null if head 
	 * is version 0 or previous version were wiped */
	private static final String API_OBJECT_GET_PREVIOUS_VERSION				[] = {"object", "getpreviousversion"}; 
	
	private static final String API_OBJECT_EXISTS							[] = {"object", "exists"};
	private static final String API_OBJECT_DELETE							[] = {"object", "delete"};
	private static final String API_OBJECT_DELETE_ALL_PREVIOUS_VERSION		[] = {"object", "deleteallpreviousversion"};
																
	private static final String API_OBJECT_GET_PRESIGNEDURL					[] = {"object", "get", "presignedurl"};
	private static final String API_OBJECT_GET_METADATA						[] = {"object", "getmetadata"};
											
	private static final String API_OBJECT_GET_HAS_VERSIONS					[] = {"object", "hasversions"};
	private static final String API_OBJECT_GET_METADATA_PREVIOUS_VERSION	[] = {"object", "getmetadatapreviousversion"};
	
												
	private static final String API_OBJECT_GET_METADATA_VERSION_ALL			[] = {"object", "getmetadatapreviousversionall"};
	private static final String API_OBJECT_URL_PRESIGNES_PREFIX				[] = {"presigned", "object"};
	
	private static final String API_OBJECT_UPLOAD 							[] = {"object", "upload"};
	
	private static final String API_OBJECT_RESTORE_PREVIOUS_VERSION 		[] = {"object", "restorepreviousversion"};

	
	/** ---------------------------------------------------- */
	
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final int HTTP_CACHE_SIZE = 200 * 1024 * 1024; // 200 mb
	
    /** default network I/O timeout is 15 minutes */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15 * 60;
	public static final String DEFAULT_USER_AGENT = "Odilon (" + System.getProperty("os.arch") + "; " + System.getProperty("os.arch") + ") odilon-java/" + OdilonClientProperties.INSTANCE.getVersion();
	
	/** default expiration for a presigned URL is 7 days in seconds */
	private static final int DEFAULT_EXPIRY_TIME = SharedConstant.DEFAULT_EXPIRY_TIME;
	  
	private static final String APPLICATION_JSON = "application/json";
	private static final DateTimeFormatter http_date = DateTimeFormatter.RFC_1123_DATE_TIME;	  
	  
	/** private static final String NULL_STRING = "(null)"; */
	private static final String END_HTTP = "----------END-HTTP----------";

	private static final String linux_home   = (new File(System.getProperty("user.dir"))).getPath();
	private static final String windows_home = System.getProperty("user.dir");

	/** private static final String UPLOAD_ID = "uploadId";
		the current client instance's base URL. */
	private HttpUrl baseUrl;
	
	private final String urlStr;
	
	/** access key to sign all requests with */
	 private String accessKey;

	 /** Secret key to sign all requests with */
	 private String secretKey;
	  
	 private String userAgent = DEFAULT_USER_AGENT;

	 private OkHttpClient httpClient;
	 private PrintWriter traceStream;
	  
	 private final OffsetDateTime created = OffsetDateTime.now();
	 private final ObjectMapper objectMapper = new ObjectMapper();

	 private int chunkSize=0;
	 
	 private boolean isLogStream = false;

	 private String charset = Charset.defaultCharset().name();
	 
	 private RandomIDGenerator rand = new RandomIDGenerator();
	 
	/***
	 * 
	 * <p>By default the server has the following settings in file {@code odilon.properties}</p>
	 * <p>Note that these parameters can not be defined or modified by the client. They are configured on the server's odilon.properties file</p> 
	 * <ul>
	 * <li>port. 9234</li>
	 * <li>accessKey. "odilon"</li>
	 * <li>secretKey. "odilon"</li>
	 * </ul>
	 * 
	 * @param endpoint 	can not be null 
	 * @param port		can not be null (normally default port is 9234)
	 * @param accessKey can not be null (default is "odilon")
	 * @param secretKey can not be null (default is "odilon")
	 * 
	 */
	public ODClient(String endpoint, int port, String accessKey, String secretKey)  {
					this(endpoint, port, accessKey, secretKey, false);
		}



	/**
	 * 
	 * @param endpoint 	can not be null
	 * @param port		can not be null (normally default port is 9234)
	 * @param accessKey can not be null
	 * @param secretKey can not be null
	 * 
	 * @param secure not used in v1.7 or lower
	 */
	protected ODClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)  {
		
			  Check.requireNonNullStringArgument(endpoint,  "endpoint is null or emtpy");
			  Check.requireNonNullStringArgument(accessKey, "accessKey is null or emtpy");
			  Check.requireNonNullStringArgument(secretKey, "secretKey is null or emtpy");

			  if (port < 0 || port > 65535) 
			      throw new IllegalArgumentException("port must be in range of 1 to 65535 -> " + String.valueOf(port));

			  this.objectMapper.registerModule(new JavaTimeModule());
			  this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			  List<Protocol> protocol = new ArrayList<>();
		      protocol.add(Protocol.HTTP_1_1);
		      
		      File cacheDirectory = new File(getCacheWorkDir());
		      Cache cache = new Cache(cacheDirectory, HTTP_CACHE_SIZE);
		      
		      this.httpClient = new OkHttpClient();
		      
		      this.httpClient = this.httpClient.newBuilder()
		        .connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
		        .writeTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
		        .readTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
		        .protocols(protocol)
		        .cache(cache)
		        .build();
		      
		      this.urlStr = endpoint;
		      
		      HttpUrl url = HttpUrl.parse(this.urlStr);
			  
			  if (url != null) {

					  if (!"/".equals(url.encodedPath())) {
				        	  throw new IllegalArgumentException("no path allowed in endpoint -> " + endpoint);
				      }

			          HttpUrl.Builder urlBuilder = url.newBuilder();
			          Scheme scheme = (secure) ? Scheme.HTTPS : Scheme.HTTP;
			          urlBuilder.scheme(scheme.toString());
			          if (port > 0)
			            urlBuilder.port(port);
			          this.baseUrl = urlBuilder.build();
			          this.accessKey = accessKey;
			          this.secretKey = secretKey;
			          return;
			    }

			    /** endpoint may be a valid hostname, IPv4 or IPv6 address */
			    if (!this.isValidEndpoint(endpoint))
			      throw new IllegalArgumentException("invalid host -> " +  endpoint);
			    
			    Scheme scheme = (secure) ? Scheme.HTTPS : Scheme.HTTP;

			    if (port == 0) {
			      this.baseUrl = new HttpUrl.Builder()
			          .scheme(scheme.toString())
			          .host(endpoint)
			          .build();
			    } else {
			      this.baseUrl = new HttpUrl.Builder()
			          .scheme(scheme.toString())
			          .host(endpoint)
			          .port(port)
			          .build();
			    }
			    this.accessKey = accessKey;
			    this.secretKey = secretKey;
		  }
		  
	
	/**
	 * 
	 */
	@Override
	public ObjectMetadata putObjectStream(String bucketName, String objectName,InputStream stream,String fileName) throws ODClientException {
		return putObjectStream(bucketName,	objectName,	stream,	Optional.ofNullable(fileName),Optional.empty());
	}

	/**
	 * 
	 */
	@Override
	public ObjectMetadata putObjectStream(String bucketName,String objectName,	InputStream stream,	Optional<String> fileName, Optional<Long> size) throws ODClientException {
		return putObjectStream(bucketName,objectName,stream,fileName,size,Optional.empty());
	}


	/**
	 * 
	 * @param bucketName
	 * @param objectName
	 * @param objectVersion
	 * @param fileName
	 * @param version
	 */
	public void putObjectStreamVersion(String bucketName, String objectName, InputStream objectVersion, String fileName, int version) {
		throw new RuntimeException("not implemented");
	}
	

	@Override
	public boolean isValidObjectName(String objectName ) {
		Check.requireNonNullStringArgument(objectName, "objectName can not be null or emtpy");
		
		if (!objectName.matches(SharedConstant.object_valid_regex))
			return false;
		
		if (objectName.length()>SharedConstant.MAX_OBJECT_CHARS)
			return false;
		
		return true;
	}
	
	
	/**
	 * 
	 */
	@Override
	public ObjectMetadata putObjectStream(String bucketName,String objectName,InputStream stream, Optional<String> fileName, Optional<Long> size, Optional<String> contentType) throws ODClientException {
		
		if (!objectName.matches(SharedConstant.object_valid_regex)) 
			throw new IllegalArgumentException(	"objectName must be >0 and <="+String.valueOf(SharedConstant.MAX_OBJECT_CHARS) + ", and must match the java regex ->  " + SharedConstant.object_valid_regex + " | o:" +	objectName);

		ObjectMetadata meta = null;
		
		//-----------
									
		String plainCredentials = accessKey + ":" + secretKey;
		
		String cType = null;
		
		if (contentType.isPresent())
			cType = contentType.get();
		else if (fileName.isPresent()) 
			cType = getContentType(fileName.get());
		else
			cType = DEFAULT_CONTENT_TYPE;
		
		HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
		  
		for ( String str: API_OBJECT_UPLOAD ) 
			urlBuilder.addEncodedPathSegment(str);
		
		urlBuilder.addEncodedPathSegment(bucketName);
		urlBuilder.addEncodedPathSegment(objectName);
		
		urlBuilder.addEncodedQueryParameter("fileName", fileName.orElse(objectName));
		urlBuilder.addEncodedQueryParameter("Content-Type", cType);

		
		HttpMultipart request = new HttpMultipart(urlBuilder.toString(), plainCredentials, this.getCharset());
		
		if (getChunkSize()>0) 
			 request.setChunk(getChunkSize());
		
		try (InputStream is = (stream instanceof BufferedInputStream) ? stream : (new BufferedInputStream(stream))) {
			meta = request.exchange(new HttpFileEntity(is, objectName, size.orElse(Long.valueOf(-1).longValue())), new TypeReference<ObjectMetadata>() {});
		} catch (IOException e) {
			throw new ODClientException(e);
		}
		return meta;
	}

	
	/**
	 * <p>Uploads the {@link File} file to the server</p>
	 * 
	 * <b>Example</b>: 
	 * 
	 * <pre>{@code 
	 * String endpoint = "http://localhost"; 
	 * // default port 
	 *  int port = 9234; 
	
	 * // default credentials 
	 * String accessKey = "odilon";
	 * String secretKey = "odilon";
			
	 * String bucketName  = "demo_bucket";
	 * String objectName1 = "demo_object1";
	 * String objectName2 = "demo_object2";
			
	 * File file1 = new File("test1.pdf");
	 * File file2 = new File("test2.pdf");
			
	 * // put two objects in the bucket
	 * // the bucket must exist before sending the object,
	 * // and object names must be unique for that bucket  
			
	 * OdilonClient client = new ODClient(endpoint, port, accessKey, secretKey);

	 * client.putObject(bucketName, objectName1, file1);
	 * client.putObject(bucketName, objectName2, file2);
	 *}
	 *</pre>
	 */
	@Override													
	public ObjectMetadata putObject(String bucketName, String objectName, File file) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		Check.requireNonNullStringArgument(objectName, "objectName can not be null | b:"+ bucketName);
		Check.requireNonNullArgument(file, "file is null");
		Check.requireTrue(file.exists(), "file does not exist");
		Check.requireTrue(!file.isDirectory(), "file can not be a Directory");
		
		return putObjectInternal(bucketName, objectName, file, file.getName());
	}
	
	
	@Override
	public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, String fileName, String contentType) throws ODClientException {
		return putObjectStream(bucketName, objectName, stream, Optional.ofNullable(fileName), Optional.empty(), Optional.ofNullable(contentType));
	}											

	
	@Override
	public ResultSet<Item<ObjectMetadata>> listObjects(Bucket bucket) throws ODClientException {
		Check.requireNonNullArgument(bucket, "bucket is null or empty");
		return listObjects(bucket.getName(), Optional.empty(), Optional.empty());
	}
	
	/**
	 * <p>
	 * {@link Item} is a wrapper for Lists and other {@link Iterable} structures of T where some elements may not be a T but an error.<br/>
	 * {@code T} must be Serializable
	 * </p>
	 * Example list all bucket's objects:
	 * <pre> {@code 
	 * try {
	 *	ResultSet<Item <ObjectMetadata>> resultSet = client.listObjects(bucket.getName());
	 *		while (resultSet.hasNext()) {
	 *			Item item = resultSet.next();
	 *			if (item.isOk())
	 *				System.out.println("ObjectName:" +  item.getObject().objectName + " | file: " + item.getObject().fileName);
	 *			else
	 *				System.out.println(item.getErrorString());
	 *		}
	 *	} catch (ODClientException e) {
	 *	   	System.out.println(String.valueOf( e.getHttpStatus())+ " " + e.getMessage() + " " + String.valueOf(e.getErrorCode()) );
	 *	}
	 *}
	 * </pre>
	 */
	@Override
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName) throws ODClientException {
			return listObjects(bucketName, Optional.empty(), Optional.empty());
	}
	
	
	@Override
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName, String prefix) throws ODClientException {
			return listObjects(bucketName, Optional.of(prefix), Optional.empty());
	}
	@Override
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName, final Optional<String> prefix, final Optional<Integer> pageSize) throws ODClientException {
		
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		
		ResultSet<Item<ObjectMetadata>> results = new ResultSet<Item<ObjectMetadata>> (
				new CachedDataProvider<Item<ObjectMetadata>> () {
					private static final long serialVersionUID = 1L;
					@Override
					protected DataList<Item<ObjectMetadata>> fetch(Optional<Long> offset, Optional<String> serverAgentId) throws IOException {
						HttpResponse httpResponse = null;
						try {
								Map<String, String> reqParams=new HashMap<String, String>();
								offset.ifPresent		( x -> reqParams.put("offset", String.valueOf(x)));
								serverAgentId.ifPresent	( x -> reqParams.put("serverAgentId", x)); 
								pageSize.ifPresent		( x -> reqParams.put("pageSize", String.valueOf(x)));
								prefix.ifPresent		( x -> reqParams.put("prefix", x));
								Multimap<String, String> queryParamMultiMap = Multimaps.forMap(reqParams);
								httpResponse = executeReq(API_BUCKET_LIST_OBJECTS, Method.GET, Optional.of(bucketName), Optional.empty(), null, queryParamMultiMap);
								
						} catch (ODClientException e) {
							throw new InternalCriticalException(e, "Error executing " + Request.class.getName());
						}
						String str = null;
						try {
							  str = httpResponse.body().string();
						 } catch (IOException e) {
							throw new InternalCriticalException(e, "Error reading Response from " + HttpResponse.class.getName());
						 }  
						
						 try {
							  TypeReference<DataList<Item<ObjectMetadata>>> ref = new TypeReference<DataList<Item<ObjectMetadata>>>() {};
							  DataList<Item<ObjectMetadata>> rl = (DataList<Item<ObjectMetadata>>) getObjectMapper().readValue(str, ref);
							  setCacheKey(rl.getAgentId());
							  if (pageSize.isPresent()) {
								  if (rl.getList().size()<pageSize.get()) {
									  setDone(true);
								  }
							  }
						  return rl;
						} catch (JsonProcessingException e) {
							throw new InternalCriticalException(e, "Error mapping response JSON to " + DataList.class.getSimpleName() + " object");
						}
					}
					
					@Override
					public String toString() {
						return this.getClass().getName() + " " +( Optional.ofNullable(getCacheKey()).isPresent()?getCacheKey():"");
					}
		
				}
		);
		return results;
	}

  	  /**
	   * <p>Returns all buckets, sorted alphabetically</p>
	   * <p><b>Example:</b></p>
	   * <pre>{@code 
	   * 	List<Bucket> bucketList = odilonClient.listBuckets();
	   * 	for (Bucket bucket : bucketList) {
	   *   		System.out.println(bucket.creationDate() + ", " + bucket.name());
	   * 	} 
	   * }</pre>
	   * 
	   *
	   * @return List of buckets
	   * @throws ODClientException
	   */
	  @Override
	  public List<Bucket> listBuckets() throws ODClientException {
		  HttpResponse httpResponse = executeReq(API_BUCKET_LIST, Method.GET);
		  String str = null;
		  try {
			  	str = httpResponse.body().string();
		  } catch (IOException e) {
				throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
		  }
		  
		  try {
					List<Bucket> list = this.objectMapper.readValue(str, new TypeReference<List<Bucket>>(){}); 
					list.sort( new Comparator<Bucket>() {
						@Override
						public int compare(Bucket o1, Bucket o2) {
							return o1.getName().compareToIgnoreCase(o2.getName());
						}
					});
					return list;
						
			} catch (JsonProcessingException e) {
					throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
			}
	  }
	  

  	  /**
	   * <p>Returns all previous versions of an Object, sorted by version number</p>
	   *
	   * @return List of ObjectMetadata sorted by version number
	   * @throws ODClientException
	   */
	  @Override
	  public List<ObjectMetadata> getObjectMetadataPreviousVersionAll(String bucketName, String objectName) throws ODClientException {
		  
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
			
		HttpResponse httpResponse = executeReq(API_OBJECT_GET_METADATA_VERSION_ALL, Method.GET, Optional.of(bucketName), Optional.of(objectName));
		  
		String str = null;
		
		try {
			  	str = httpResponse.body().string();
		} catch (IOException e) {
				throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		 
		try {
			List<ObjectMetadata> list = this.objectMapper.readValue(str, new TypeReference<List<ObjectMetadata>>(){});
					list.sort( new Comparator<ObjectMetadata>() {
						@Override
						public int compare(ObjectMetadata o1, ObjectMetadata o2) {
							if (o1.version < o2.version) return -1;
							if (o1.version == o2.version)return 0;
							return 1;
						}
					});
					return list;
						
			} catch (JsonProcessingException e) {
					throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
			}
	  }

	  
	  /**
	   * 
	   */
	  @Override
	  public boolean isEmpty(String bucketName) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		HttpResponse httpResponse = executeReq(API_BUCKET_ISEMPTY, Method.GET, Optional.of(bucketName));
		try {	
				return httpResponse.body().string().equals("true");
		} catch (IOException e) {
				throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}
	 
	  
	  /**
	   * 
	   */
	@Override		 
	public boolean existsBucket(String bucketName) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		HttpResponse httpResponse = executeReq(API_BUCKET_EXISTS, Method.GET, Optional.of(bucketName));
		try {
			String res=httpResponse.body().string();
			return res.equals("true");
		} catch (IOException e) {
			logger.error("existsBucket error");
			throw new ODClientException(e);
		}
	}
	
	/**
	 * 
	 */
	 @Override
	 public Bucket getBucket(String bucketName) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		String str = null;
		try {
			 HttpResponse httpResponse = executeReq(API_BUCKET_GET, Method.GET, Optional.of(bucketName));
			 str = httpResponse.body().string();
		} catch (IOException e) {
				throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		try {
			return this.objectMapper.readValue(str, Bucket.class);
		} catch (JsonProcessingException e) {
			throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), "error parsing Bucket from Server response"+ " | " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}

	 /**
	  * 
	  */
	@Override
	public void createBucket(String bucketName) throws ODClientException  {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		 
		if (bucketName.length()<1 || bucketName.length()>SharedConstant.MAX_BUCKET_CHARS) {
			throw new IllegalArgumentException( "bucketName must be >0 and <" + String.valueOf(SharedConstant.MAX_BUCKET_CHARS) + "' | b:" + bucketName);
		}
		if (!bucketName.matches(SharedConstant.bucket_valid_regex)) { 
			throw new IllegalArgumentException("bucketName must match java regex = '" + SharedConstant.bucket_valid_regex + "' | b:" + bucketName);
		}
		
		HttpResponse response = executePost(API_BUCKET_CREATE, Optional.of(bucketName), Optional.empty(), null, null, "", 0, false);
		response.body().close();
	}
	

	 /**
	  * 
	  
	@Override
	public void renameBucket(String bucketName, String newBucketName) throws ODClientException  {
		
		Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		Check.requireNonNullStringArgument(newBucketName, "newBucketName is null");
		
		if (newBucketName.length()<1 || newBucketName.length()>SharedConstant.MAX_BUCKET_CHARS) {
			throw new IllegalArgumentException( "newBucketName must be >0 and <" + String.valueOf(SharedConstant.MAX_BUCKET_CHARS) + "' | b:" + newBucketName);
		}
		if (!newBucketName.matches(SharedConstant.bucket_valid_regex)) { 
			throw new IllegalArgumentException("newBucketName must match java regex = '" + SharedConstant.bucket_valid_regex + "' | b:" + newBucketName);
		}
		
		
		HttpResponse response = executePost(API_BUCKET_RENAME, Optional.of(bucketName), Optional.of(newBucketName), null, null, "", 0, false);
		response.body().close();
	}
*/
	
	
	
	/**
	 * 
	 * @param requestClass
	 * @throws ODClientException
	 */
	public void addServiceRequest(String requestClass) throws ODClientException  {
		Check.requireNonNullStringArgument(requestClass, "requestClass is null");
		HttpResponse response = executePost(API_SERVICE_REQUES_ADD, Optional.of(requestClass), Optional.empty(), null, null, "", 0, false);
		response.body().close();
	}

	
	/**
	 * 
	 */
	@Override
	public void deleteBucket(String bucketName) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		HttpResponse response = executeDelete(API_BUCKET_DELETE, Optional.of(bucketName), Optional.empty(), null);
		response.body().close();
	}
	
	 /** 
	   *<p> Removes an object from a bucket.</p>
	   *
	   * <p><b>Example:</b></p>
	   * <pre>{@code 
	   * 	odilonClient.deleteObject("my-bucketname", "my-objectname"); 
	   * }</pre>
	   * 
	   *
	   * @param bucketName Bucket name
	   * @param objectName Object name in the bucket
	   **/
	 @Override
	 public void deleteObject(String bucketName, String objectName) throws ODClientException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 HttpResponse response = executeDelete(API_OBJECT_DELETE, Optional.of(bucketName), Optional.of(objectName), null);
		 response.body().close();
	 }

	 
	 /**
	  * 
	  * 
	  */
	 @Override
	 public void deleteObjectAllVersions(String bucketName, String objectName) throws ODClientException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 
		 if (!isVersionControl())
			 throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.API_NOT_ENABLED.getCode(), "Server does not support Version Control");
		 
		 HttpResponse response = executeDelete(API_OBJECT_DELETE_ALL_PREVIOUS_VERSION, Optional.of(bucketName), Optional.of(objectName), null);
		 response.body().close();
	 }

	 
	 /**
	  *
	  * 
	  */
	 @Override
	 public void restoreObjectPreviousVersions(String bucketName, String objectName) throws ODClientException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 
		 if (!isVersionControl())
			 throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.API_NOT_ENABLED.getCode(), "Server does not support Version Control");

		 HttpResponse response = executePost(API_OBJECT_RESTORE_PREVIOUS_VERSION, Optional.of(bucketName), Optional.of(objectName), null , null, "", 0, false);
		 response.body().close();
	 }

	 /**
	  * 
	  */
	 @Override
	 public void deleteAllBucketVersions(String bucketName) throws ODClientException  {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 
		 if (!isVersionControl())
			 throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.API_NOT_ENABLED.getCode(), "Server does not support Version Control");
		
		 HttpResponse response = executeDelete(API_BUCKET_DELETE_ALL_PREVIOUS_VERSION, Optional.of(bucketName), Optional.empty(), null);
		 response.body().close();
	 };
	 
	 /**
	  * <p>Returns {@link SystemInfo}</p>
	  */
	 @Override
	 public SystemInfo systemInfo() throws ODClientException {
		HttpResponse httpResponse = executeReq(API_SYSTEM_INFO, Method.GET);
		String str = null;
	  	try {
	  		str = httpResponse.body().string();
		} catch (IOException e1) {
			throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e1.getClass().getSimpleName() + " - " + e1.getMessage());
		}
	  	try {
				return this.objectMapper.readValue(str, SystemInfo.class);
		} catch (JsonProcessingException e) {
				throw new ODClientException(	ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(),"error parsing " + MetricsValues.class.getName() + " from Server response"+ " | " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}

	 /**
	  * 
	  */
	 @Override
	 public boolean isVersionControl() throws ODClientException {
		 return systemInfo().isVersionControl(); 
	 }

	 @Override
	 public MetricsValues metrics() throws ODClientException {
		HttpResponse httpResponse = executeReq(API_METRICS, Method.GET);
		String str = null;
	  	try {
	  		str = httpResponse.body().string();
		} catch (IOException e1) {
			throw new ODClientException(ODHttpStatus.OK.value(), ErrorCode.INTERNAL_ERROR.getCode(), e1.getClass().getSimpleName() + " - " + e1.getMessage());
		}
	  	try {
				return this.objectMapper.readValue(str, MetricsValues.class);
		} catch (JsonProcessingException e) {
				throw new ODClientException(	ODHttpStatus.OK.value(), 	ErrorCode.INTERNAL_ERROR.getCode(),	"error parsing " + MetricsValues.class.getName() + " from Server response"+ " | " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}

	  /**
	   * <p>Returns the String "ok" or a String with the error reported by the Server. 
	   * If the client can not connect to the Server, the method returns a message "can not connect"</p>
	   * 
	   * <b>Example:</b><br>
	   * <pre>{@code 
	   * String pingResult = odilonClient.ping();
	   * if (!pingResult.equals("ok")) {
	   *   System.out.println( "Server error -> " + pingResult));
	   * }
	   * }</pre>
	  * 
	  * @return String "ok" or the error reported by the Server.
	  */
	@Override
	public String ping() {
		  try {
			  
			  HttpResponse httpResponse = executeReq(API_PING, Method.GET);
			  return httpResponse.body().string();
			  
		  } catch (ODClientException e) {
			  return e.toString();
		  } catch (IOException e1) {
				logger.error(e1);
				throw new InternalCriticalException(e1);
		  }
	  }

	
	/**
	 * <p>A presigned URL is a way to grant temporary access to an Object, for example in an HTML webpage.
	 *  It remains valid for a limited period of time which is specified when the URL is generated.
	 * </p>
	 * */
	  public String getPresignedObjectUrl(String bucketName, String objectName, Optional<Integer> expires, Map<String, String> reqParams) throws ODClientException {
	 		return getPresignedObjectUrl(Method.GET,bucketName, objectName, expires, reqParams); 
	  }
	 	
	  /**
		 * <p>A presigned URL is a way to grant temporary access to an Object, for example in an HTML webpage.
		 *  It remains valid for a limited period of time which is specified when the URL is generated.
		 * </p>
		 * */
	  public String getPresignedObjectUrl(String bucketName, String objectName, Optional<Integer> expires)  throws ODClientException {
	 		return getPresignedObjectUrl(Method.GET,bucketName, objectName, expires, null);
	  }
	 	
	  /**
	   * <p>Returns an presigned URL to download the binary object 
	   * with default expiry time. <br/> 
	   * Default expiry time is 7 days in seconds.</p>
	   */
	 
	  @Override
	  public String getPresignedObjectUrl(String bucketName, String objectName) throws ODClientException {
	 		return getPresignedObjectUrl(bucketName, objectName, Optional.empty(), null);	
	 }	  
	  
	  @Override
	  public boolean existsObject(String bucketName, String objectName) throws ODClientException, IOException {
		  	Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
			Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
			HttpResponse httpResponse = executeReq(API_OBJECT_EXISTS, Method.GET, Optional.of(bucketName), Optional.of(objectName));
			return httpResponse.body().string().equals("true");
	  }
	
	  @Override
	  public boolean hasVersions(String bucketName, String objectName) throws ODClientException, IOException {
		  		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
				Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
				HttpResponse httpResponse = executeReq(API_OBJECT_GET_HAS_VERSIONS, Method.GET, Optional.of(bucketName), Optional.of(objectName));
				return httpResponse.body().string().equals("true");
	 }
	  
	  /** 
	  * <p>Returns {@link ObjectMetadata} of given object in given bucket.<p>
	  * 
	  *  <b>Example:</b><br>
	  * <pre>{@code ObjectMetadata meta = odilonClient.getObjectMetadata("my-bucketname", "my-objectname");
	  *   			System.out.println(meta.toString()));}</pre>
	  *
	  * @param bucketName Bucket name
	  * @param objectName Object name in the bucket
	  * */
	  @Override
	  public ObjectMetadata getObjectMetadata(String bucketName, String objectName) throws ODClientException {
		  	Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
			Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
			HttpResponse httpResponse = executeReq(API_OBJECT_GET_METADATA, Method.GET, Optional.of(bucketName), Optional.of(objectName));
			String str = null;
		  	try {
			  		str = httpResponse.body().string();
			} catch (IOException e1) {
					throw new InternalCriticalException(e1);
			}
		  	try {
					return this.objectMapper.readValue(str, ObjectMetadata.class);
			} catch (JsonProcessingException e) {
						throw new InternalCriticalException(e);
			}
	  }
	  
	  /** 
		  * <p>Returns {@link ObjectMetadata} of previous version <p>
		  * <b>Example:</b><br>
		   * <pre>{@code ObjectMetadata meta = odilonClient.getObjectMetadata("my-bucketname", "my-objectname");
		   *   			System.out.println(meta.toString()));}</pre>
		   *
		   * @param bucketName Bucket name.
		   * @param objectName Object name in the bucket.
		   *
		  * */
		  @Override								
		  public ObjectMetadata getObjectMetadataPreviousVersion(String bucketName, String objectName) throws ODClientException {
			  	
			  Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
			  Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
				
			  HttpResponse httpResponse = executeReq(API_OBJECT_GET_METADATA_PREVIOUS_VERSION, Method.GET, Optional.of(bucketName), Optional.of(objectName));
			  String str = null;
			  	
			  try {
				  		
			  		str = httpResponse.body().string();
			  		
			  		if (str==null)
			  			return null;
				  		
			  } catch (IOException e1) {
					logger.error(e1);
					throw new InternalCriticalException(e1);
			  }
			  
			  try {
					
			  		return this.objectMapper.readValue(str, ObjectMetadata.class);
			  		
			  } catch (JsonProcessingException e) {
							throw new InternalCriticalException(e);
			  }
		  }
 /** 
   *<p> 
   * Gets entire object's data as {@link InputStream} in given bucket. The InputStream must be closed
   * after use else the connection will remain open.
   *</p>
   * 
   * <b>Example:</b>
   * <pre>{@code InputStream stream = OdilonClient.getObject("my-bucketname", "my-objectname");
   * byte[] buf = new byte[16384];
   * int bytesRead;
   * while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
   *   System.out.println(new String(buf, 0, bytesRead));
   * }
   * stream.close(); }</pre>
   *
   * @param bucketName Bucket name
   * @param objectName Object name in the bucket
   *
   * @return {@link InputStream} containing the object data.
   */
	  @Override
	  public InputStream getObject(String bucketName, String objectName) throws ODClientException {
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		HttpResponse httpResponse = executeReq(API_OBJECT_GET, Method.GET, Optional.of(bucketName), Optional.of(objectName));
		return httpResponse.body().byteStream();
	 }
	 
	  
	  @Override
	 public InputStream getObjectPreviousVersion(String bucketName, String objectName) throws ODClientException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 HttpResponse httpResponse = executeReq(API_OBJECT_GET_PREVIOUS_VERSION, Method.GET, Optional.of(bucketName), Optional.of(objectName));
		return httpResponse.body().byteStream();
	 }
	  
	 /**
	  * <p> get InputStream of the version passed as parameter, 
	  *  null if head is version 0 
	  *  parameter is non existent
	  *  or previous versions were wiped </p>
	  */
	  @Override
	 public InputStream getObjectVersion(String bucketName, String objectName, int version) throws ODClientException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 Map<String, String> reqParams = new HashMap<String, String>();
		 reqParams.put("version", String.valueOf(version));
		 Multimap<String, String> queryParamMultiMap = Multimaps.forMap(reqParams);
			
		HttpResponse httpResponse = executeReq(API_OBJECT_GET_VERSION, Method.GET, Optional.of(bucketName), Optional.of(objectName), null, queryParamMultiMap);
		return httpResponse.body().byteStream();
	 }
	  
	  
	/**
	   * <p>Gets object's data and stores it to given file name.</p>
	   * <b>Example:</b><br>
	   * <pre>{@code odilonClient.getObject("my-bucketname", "my-objectname", "photo.jpg"); }</pre>
	   *
	   * @param bucketName  Bucket name.
	   * @param objectName  Object name in the bucket.
	   * @param fileName    file name.
	   */
	 @Override
	 public void getObject(String bucketName, String objectName, String fileName) throws ODClientException, IOException {
		 Check.requireNonNullStringArgument(bucketName, "bucketName is null");
		 Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);
		 Check.requireNonNullStringArgument(fileName, "fileName is null");
		 transferTo(getObject(bucketName, objectName), fileName);
		
	 }
	  
	  /**
	   * <p>Sets HTTP connect, write and read timeouts.  A value of 0 means no timeout, otherwise values must be between 1 and
	   * Integer.MAX_VALUE when converted to milliseconds.</p>
	   * <b>Example:</b><br>
	   * <pre>{@code odilonClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
	   *                            TimeUnit.SECONDS.toMillis(30)); }</pre>
	   *                            
	   * @param connectTimeoutMilliseconds    HTTP connect timeout in milliseconds.
	   * @param writeTimeoutMilliseconds      HTTP write timeout in milliseconds.
	   * @param readTimeoutMilliseconds       HTTP read timeout in milliseconds.
	   */
	  
	 	@Override
	 	public void setTimeout(long connectTimeoutMilliseconds, long writeTimeoutMilliseconds, long readTimeoutMilliseconds) {
	    this.httpClient = this.httpClient.newBuilder()
	      .connectTimeout(connectTimeoutMilliseconds, TimeUnit.MILLISECONDS)
	      .writeTimeout(writeTimeoutMilliseconds, TimeUnit.MILLISECONDS)
	      .readTimeout(readTimeoutMilliseconds, TimeUnit.MILLISECONDS)
	      .build();
	  }

	  /**
	   *<p>Ignores check on server certificate for HTTPS connection.</p>
	   *<b>Example:</b><br>
	   * <pre>{@code odilonClient.ignoreCertCheck(); }</pre>
	   */
	  public void ignoreCertCheck() throws NoSuchAlgorithmException, KeyManagementException {
	    final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	        @Override
	        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	        }

	        @Override
	        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	        }

	        @Override
	        public X509Certificate[] getAcceptedIssuers() {
	          return new X509Certificate[]{};
	        }
	      }
	    };

	    final SSLContext sslContext = SSLContext.getInstance("SSL");
	    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
	    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

	    this.httpClient = this.httpClient.newBuilder()
	      .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
	      .hostnameVerifier(new HostnameVerifier() {
	          @Override
	          public boolean verify(String hostname, SSLSession session) {
	            return true;
	          }
	        })
	      .build();
	  }

	  @Override
	  public boolean isValidBucketName(String name) {
		  	
		  	Check.requireNonNullStringArgument(name, "bucketName is null or empty");
		  	
		  	if (name.length()<1 || name.length()>SharedConstant.MAX_BUCKET_CHARS)
		  		return false;
				
			if (name.matches("\\.\\."))
			   	return false;
				
			if (!name.matches(SharedConstant.bucket_valid_regex))
				return false;
		  	
		    return true;
	  }
	  
	  /**
	   * <p>Validates if given bucket name is DNS compatible.
	   * This method was part of the Minio Client SDK 1.4</p>
	   *
	   *
	   * @throws InvalidBucketNameException  upon invalid bucket name is given
	   * 
	   */
	  private void checkBucketName(String bucketName) {
		  
		Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");

		if (bucketName.length()<1 || bucketName.length()>SharedConstant.MAX_BUCKET_CHARS)
			throw new IllegalArgumentException( "bucketName must be >0 and <" + String.valueOf(SharedConstant.MAX_BUCKET_CHARS));
	    
	    // Successive periods in bucket names are not allowed.
	    if (bucketName.matches("\\.\\.")) 
	    	  throw new IllegalArgumentException("bucket name cannot contain successive periods -> " + bucketName);
	    
	    if (!bucketName.matches(SharedConstant.bucket_valid_regex)) 
	    	  throw new IllegalArgumentException("bucket name not valid -> " + bucketName);
	  }
	  
	  /**
	   * <p>Checks whether port should be omitted in Host header.
	   * HTTP Spec (rfc2616) defines that port should be omitted in Host header
	   * when port and service matches (i.e HTTP -> 80, HTTPS -> 443)
	   *</p>
	   * @param url Url object
	   */
	  private boolean shouldOmitPortInHostHeader(HttpUrl url) {
	    return (url.scheme().equals("http") && url.port() == 80)
	      || (url.scheme().equals("https") && url.port() == 443);
	  }
	  
	  
	  /*
	   * Enables HTTP call tracing and written to traceStream.
	   * @param traceStream {@link OutputStream} for writing HTTP call tracing.
	   * @see #traceOff
	   */
	  public void traceOn(OutputStream traceStream) {
	    if (traceStream == null) 
	    	 throw new IllegalArgumentException("OutputStream can not be null");
	      this.traceStream = new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
	  }
	  
	  /**
	   * <p>Disables HTTP call tracing previously enabled.
	   * </p>
	   * 
	   * @see #traceOn
	   * @throws IOException upon connection error
	   */
	  public void traceOff() throws IOException {
	    this.traceStream = null;
	  }


	  
	public String toJSON() {
			StringBuilder str = new StringBuilder();
			str.append("\"url\":\"" +baseUrl.toString()+ "\"");
			str.append(", \"accessKey\":\"" + accessKey + "\"");
			str.append(", \"secretKey\":\"" + secretKey + "\"");
			return str.toString();
		}
		

	@Override
	public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(this.getClass().getSimpleName() +"{");
			str.append(toJSON());
			str.append("}");
			return str.toString();
	}

	public  OffsetDateTime created() {
	  return created;
	}

	
	
	@Override
	public void setChunkSize(int chunkSize) {
			this.chunkSize=chunkSize;
	}
	
	
	@Override
	public int getChunkSize() {
		return this.chunkSize;
	}

	@Override
	public void setCharset(String c) {
		 this.charset=c;
	 }

	@Override
	public String getCharset() {
		 return charset;
	 }

	 
	@Override
	public String getUrl() {
		return urlStr;
	}

	
	protected ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}
 
 
	private void transferTo(InputStream stream, String destFileName) throws IOException {
 			
			Check.requireNonNullArgument(stream, "stream is null");
		
 	 		byte[] buf = new byte[ BUFFER_SIZE ];
 	 		int bytesRead;
 			BufferedOutputStream out = null;
 			IOException eThrow = null;
 			
 			try (stream) {
 				out = new BufferedOutputStream(new FileOutputStream(destFileName), BUFFER_SIZE);
 				while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
 					  out.write(buf, 0, bytesRead);
 				}
 			} catch (IOException e) {
 				eThrow=e;
 				throw (e);		

 			} finally {
 				
 				if (out!=null) { 
 					try {
 						out.close();
 					} catch (IOException e) {
 						if (eThrow==null)
 							eThrow = e;
 						else
 							logger.error(e, "NOT THROWN");
 					}	
 				}
 				
 				if (eThrow !=null)
 					throw eThrow;
 			}
 		}

 
	
	/**
	   * Gets object's data of given offset and length as {@link InputStream} in the given bucket. The InputStream must be
	   * closed after use else the connection will remain open.
	   *
	   * </p><b>Example:</b><br>
	   * <pre>{@code InputStream stream = odilonClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L);
	   * byte[] buf = new byte[16384];
	   * int bytesRead;
	   * while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
	   *   System.out.println(new String(buf, 0, bytesRead));
	   * }
	   * stream.close(); }</pre>
	   *
	   * @param bucketName  Bucket name.
	   * @param objectName  Object name in the bucket.
	   * @param offset      Offset to read at.
	   * @param length      Length to read.
	   *
	   * @return {@link InputStream} containing the object's data. 
	 */

	 /**
	   * Returns a presigned URL string with given HTTP method, expiry time and custom request params for a 
	   * specific object in the bucket.
	   *
	   * </p><b>Example:</b><br>
	   * <pre>{@code String url = odilonClient.getPresignedObjectUrl(Method.GET, "my-bucketname", "my-objectname",
	   * 60 * 60 * 24, reqParams);
	   * System.out.println(url); }</pre>
	   *
	   * @param method      HTTP {@link Method}.
	   * @param bucketName  Bucket name.
	   * @param objectName  Object name in the bucket.
	   * @param expires     Expiration time in seconds of presigned URL.
	   * @param reqParams   Override values for set of response headers. Currently supported request parameters are
	   *                    [response-expires, response-content-type, response-cache-control, response-content-disposition]
	   *
	   * @return string contains URL to download the object.
	   */
	 private String getPresignedObjectUrl(	Method method, 
			 								String bucketName, 
			 								String objectName, 
			 								Optional<Integer> expiresInSeconds,  
			 								Map<String, String> reqParams) throws ODClientException {
	 		
		  	Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
		  	Check.requireNonNullStringArgument(objectName, "objectName can not be null or empty | b:"+ bucketName);

			 
			HttpResponse httpResponse = null;
			
			if (reqParams==null)
				reqParams=new HashMap<String, String>();
					
			reqParams.put("durationSeconds",String.valueOf( expiresInSeconds.orElse( DEFAULT_EXPIRY_TIME ) ));

			Multimap<String, String> queryParamMultiMap = Multimaps.forMap(reqParams);
			httpResponse = executeReq(API_OBJECT_GET_PRESIGNEDURL, Method.GET, Optional.of(bucketName), Optional.of(objectName), null, queryParamMultiMap);

			String str = null;
			
			try {
				str = httpResponse.body().string();
			} catch (IOException e) {
				throw new ODClientException(e);
			}
			
			String sPort = ((baseUrl.port()!=80 && baseUrl.port()!=443) ?  (":"+ String.valueOf(baseUrl.port())): "");
			StringBuilder url = new StringBuilder();
			url.append(baseUrl.scheme() + "://" + baseUrl.host() + sPort);
			for (String leg: API_OBJECT_URL_PRESIGNES_PREFIX) 
			  url.append("/"+leg);
			String urlEncoded = null;
			try {
				urlEncoded = URLEncoder.encode(str, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new InternalCriticalException(e);
			}
			
			return url.toString() + "?token="+ urlEncoded;
	 	}	

	
	  /**
	   * @param relativePath
	   * @param method
	   * @param bucketName
	   * @param objectName
	   * @param headerMap
	   * @param queryParamMap
	   * @param body
	   * @param length
	   * @return
	   */
	  
	  private Request createRequest(	String relativePath[], 
			  							Method method, 
			  							Optional<String> bucketName, 
			  							Optional<String> objectName, 
			  							Multimap<String,String> headerMap, 
			  							Multimap<String,String> queryParamMap,
			  							final String contentType,
			  							Object body,
			  							int length,
			  							boolean multiPart) throws NoSuchAlgorithmException, IOException {
		  
		  
		  
		  HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
		  
		  if (relativePath!=null) {
			  for (String str: relativePath) 
				  urlBuilder.addEncodedPathSegment(str);
		  }
		  
		  
		  if (bucketName.isPresent()) {
				checkBucketName(bucketName.get());
				urlBuilder.addEncodedPathSegment(bucketName.get());
		  }
		  
		  if (objectName.isPresent()) 
			  	urlBuilder.addEncodedPathSegment(objectName.get());
		   
		  if (queryParamMap != null) {
				for (Map.Entry<String,String> entry : queryParamMap.entries()) {
						urlBuilder.addEncodedQueryParameter(entry.getKey(), entry.getValue());
				}
		  }
		  
		  HttpUrl url = urlBuilder.build();

		  Request.Builder requestBuilder = new Request.Builder();
		  
		  requestBuilder.url(url);
		  
		  String sha256 = null;

		  if (body != null) {
			  // body must be byte[] or 
			  sha256 = Digest.sha256Hash(body, length);
		  }

		  if (this.accessKey != null && this.secretKey != null) {
		   	  String encoding = Base64.getEncoder().encodeToString((accessKey + ":" + secretKey).getBytes());
		   	  String authHeader = "Basic " + encoding;
		   	  requestBuilder.header("Authorization", authHeader);
		  }

		  if (sha256 != null)
			  requestBuilder.header("ETag", sha256);
		  
		  requestBuilder.header("Host", this.shouldOmitPortInHostHeader(url) ? url.host() : (url.host() + ":" + url.port()));
		  requestBuilder.header("User-Agent", this.userAgent);
		  requestBuilder.header("Accept", APPLICATION_JSON);
		  requestBuilder.header("Accept-Charset", "utf-8");
		  requestBuilder.header("Accept-Encoding", "gzip, deflate");
		  requestBuilder.header("Date", http_date.format(OffsetDateTime.now()));
		  
		  //requestBuilder.header("Content-Encoding", "gzip, deflate");
		  
		  
		  
		  
		  if (multiPart) {
			  requestBuilder.header("Transfer-Encoding", "gzip, chunked");
		  }
		  
		  if (headerMap != null) {
				for (Map.Entry<String,String> entry : headerMap.entries()) {
					requestBuilder.header(entry.getKey(), entry.getValue());
				}
		  }
		  
		  if (body != null) {
			  RequestBody requestBody = null;
			  requestBody = new HttpRequestBody(contentType, body, length);
			  if (multiPart) {
					  	String fileName = queryParamMap.get("filename").toString();
					  	MultipartBody multipartBody = new MultipartBody.Builder()
				           .setType(MultipartBody.FORM)  // Header to show we are sending a Multipart Form Data
				           .addFormDataPart("file", fileName, requestBody) // file param
				           .addFormDataPart("Content-Type", contentType) // other string params can be like userId, name or something
				           .build();
			  			requestBuilder.method(method.toString(), multipartBody);
			  }
			  else {
				  requestBuilder.method(method.toString(), requestBody);
			  }
		  }
		  else {
			  requestBuilder.method(method.toString(), null);
		  }
		  return requestBuilder.build();
	  }
	  	
	  /**
	   * @param relativePath
	   * @param method
	   * @param bucketName
	   * @param objectName
	   * @param headerMap
	   * @param queryParamMap
	   * @param body
	   * @param length
	   * @return
	   */
	private HttpResponse execute(String relativePath[], Method method, Optional<String> bucketName, Optional<String> objectName, Map<String,String> headerMap, Map<String,String> queryParamMap, Object body, int length, boolean multiPart) throws ODClientException {

		Multimap<String, String> queryParamMultiMap = null;
		
		if (queryParamMap != null) {
			queryParamMultiMap = Multimaps.forMap(queryParamMap);
		}
		Multimap<String, String> headerMultiMap = null;
		if (headerMap != null) {
			headerMultiMap = Multimaps.forMap(headerMap);
		}
		return executeReq(relativePath, method, bucketName, objectName, headerMultiMap, queryParamMultiMap, body, length, multiPart);
	}
	
	private HttpResponse executeDelete(String relativePath[], Optional<String> bucketName, Optional<String> objectName, Map<String,String> queryParamMap) throws ODClientException {
		 HttpResponse response = execute(relativePath, Method.DELETE, bucketName, objectName, null, queryParamMap, null, 0, false);
		 return response;
	}
	 
	private HttpResponse executePost(String relativePath[], Optional<String> bucketName, Optional<String> objectName, Map<String,String> headerMap, Map<String,String> queryParamMap, Object data, int length, boolean multiPart) throws ODClientException {
	 		HttpResponse response = execute(relativePath, Method.POST, bucketName, objectName, headerMap, queryParamMap, data, length, multiPart);
	 		return response;
	 }
	 
	/**
	 * 
	 * @param relativePath
	 * @param method
	 * @param bucketName
	 * @return
	 * @throws ODClientException 
	 */
	private HttpResponse executeReq(String relativePath[], Method method, Optional<String> bucketName) throws ODClientException {
		  	return executeReq(relativePath, method, bucketName, Optional.empty(), 	null, null, null, 0, false);
	}										
	 
	private HttpResponse executeReq(String relativePath[], Method method, Optional<String> bucketName, Optional<String> objectName, Multimap<String,String> headerMap, Multimap<String,String> queryParamMap) throws ODClientException {
	  	return executeReq(relativePath, method, bucketName, objectName, headerMap, queryParamMap, null, 0, false);
	}
	
	private HttpResponse executeReq(String relativePath[], Method method, Optional<String> bucketName, Optional<String> objectName)  throws ODClientException {
	  	return executeReq(relativePath, method, bucketName, objectName, null, null, null, 0, false);
	}
	
	/**
	 * @param relativePath
	 * @param method
	 * @return
	 */
    private HttpResponse executeReq(String relativePath[], Method method)  throws ODClientException {
	  	return executeReq(relativePath,method, Optional.empty(), Optional.empty(),null,null,null,0, false );
	}
	  
    /**
     * @param relativePath
     * @param method
     * @param bucketName
     * @param objectName
     * @param headerMap
     * @param queryParamMap
     * @param body
     * @param length
     * @return
     */
    private HttpResponse executeReq(String relativePath[], Method method, Optional<String> bucketName, Optional<String> objectName, 
    								Multimap<String,String> headerMap, Multimap<String, String> queryParamMap, 
    								Object body, int length, boolean multiPart) throws ODClientException {
		  
		   String contentType = null;
		  	
		   if (headerMap != null && headerMap.get("Content-Type") != null) {
			   contentType = String.join(" ", headerMap.get("Content-Type"));
		   }
		   
		   if (body != null && !(body instanceof InputStream || body instanceof RandomAccessFile || body instanceof byte[])) {
		      byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
		      body = bytes;
		      length = bytes.length;
		   }
	
		   Request request = null;
		   
		try {
			   	request = createRequest(relativePath, method, bucketName, objectName, headerMap, queryParamMap, contentType, body, length, multiPart);
			   	
		} catch (NoSuchAlgorithmException | IOException e) {
				logger.error("before sending the Request. Caused by -> request = createRequest(...)");
				throw new InternalCriticalException(e);
		}

	   	
	   if (this.traceStream != null) {
			      this.traceStream.println("---------START-HTTP---------");
			      String encodedPath = request.url().encodedPath();
			      String encodedQuery = request.url().encodedQuery();
			      if (encodedQuery != null) {
			        encodedPath += "?" + encodedQuery;
			      }
			      this.traceStream.println(request.method() + " " + encodedPath + " HTTP/1.1");
			      String headers = request.headers().toString()
			          .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
			          .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*");
			      this.traceStream.println(headers);
	   }
	   
	   
	   if (this.isLogStream && logger.isDebugEnabled()) {
		      logger.debug("---------START-HTTP---------");
		      String encodedPath = request.url().encodedPath();
		      String encodedQuery = request.url().encodedQuery();
		      if (encodedQuery != null) {
		        encodedPath += "?" + encodedQuery;
		      }
		      logger.debug(request.method() + " " + encodedPath + " HTTP/1.1");
		      String headers = request.headers().toString()
		          .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
		          .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*");
		      logger.debug(headers);
		      logger.debug();
		      
	   }
	   
	   
	   Response response;

	   try {

		   response = this.httpClient.newCall(request).execute();
	   
	   } catch (ConnectException e) {
		   throw new ODClientException(			ODHttpStatus.INTERNAL_SERVER_ERROR.value(), 
				   								ErrorCode.SERVER_UNREACHEABLE.value(), 
				   								e.getClass().getName()+ " -> " + e.getMessage()
				   								);
		   
	   } catch (IOException e) {
			throw new InternalCriticalException(e, "Caused by -> "+ OkHttpClient.class.getName());
	   }
	    
	   
	   if (this.traceStream != null) {
	      this.traceStream.println(response.protocol().toString().toUpperCase(Locale.US) + " " + response.code());
	      this.traceStream.println(response.headers());
	   }
		
	   if (this.isLogStream && logger.isDebugEnabled()) {
		   logger.debug(response.protocol().toString().toUpperCase(Locale.US) + " " + response.code());
		   logger.debug(response.headers());
	   }
	   
	   ResponseHeader header = new ResponseHeader();
	   HeaderParser.set(response.headers(), header);

	   if (response.isSuccessful()) {
	      
		   if (this.traceStream != null)
	        this.traceStream.println(END_HTTP);

	      if (this.isLogStream && logger.isDebugEnabled()) 
	 		  logger.debug(END_HTTP);
	      
	      return new HttpResponse(header, response);
	    }
	   
	   /** if response is not successful -> throw OdilonException  ---------------------------- */
	   
		   

			String str;
			
			try {
				
				str = response.body().string();
				
				if (logger.isDebugEnabled()) {
					logger.debug("error response body -> " + (str!=null?str:"null"));
			   }
				
			} catch (IOException e) {
				throw new InternalCriticalException(e, "caused by " + Response.class.getName());
			}

			int httpCode = response.code();
			
			if (httpCode==ODHttpStatus.UNAUTHORIZED.value()) 
				   throw new ODClientException(	ODHttpStatus.UNAUTHORIZED.value(), ErrorCode.AUTHENTICATION_ERROR.value(), ErrorCode.AUTHENTICATION_ERROR.getMessage());
			
			if (httpCode==ODHttpStatus.INTERNAL_SERVER_ERROR.value()) { 
				throw new ODClientException(ODHttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.INTERNAL_ERROR.value(), response.toString());
			}
			
			if (httpCode==ODHttpStatus.FORBIDDEN.value()) { 
				throw new ODClientException(ODHttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.ACCESS_DENIED.value(), response.toString());
			}
			
		   	try {
						OdilonErrorProxy proxy = this.objectMapper.readValue(str, OdilonErrorProxy.class);
				    	ODClientException ex = new ODClientException(proxy.getHttpStatus(), proxy.getErrorCode(), proxy.getMessage());
						Map<String, String> context = new HashMap<String, String>();
						context.put( "bucketName",bucketName.orElse("null"));
						context.put( "objectName",objectName.orElse("null"));
						
						if (queryParamMap!=null) {
							queryParamMap.asMap().forEach( (k,v) -> context.put(k, v.toString()) );
						}
						ex.setContext(context);
						
						throw(ex);
		    		
	    		} catch (JsonProcessingException e) {
	    			throw new InternalCriticalException(e, str!=null? str:"");
	    		}
	  }

    
	
	 /**
	  * Uses okhttp3 
	  */
	 private ObjectMetadata putObjectInternal(String bucketName, String objectName, File file, String fileName) throws ODClientException {
	 				 
	 				Check.requireNonNullStringArgument(bucketName, "bucketName is null or empty");
	 				Check.requireNonNullStringArgument(objectName, "object is null or empty");
	 				Check.requireNonNullArgument(file, "file is null"); 
	 				Check.requireNonNullStringArgument(fileName, "fileName is null");
	 				
	 				if (!objectName.matches(SharedConstant.object_valid_regex)) 
	 					throw new IllegalArgumentException(	"objectName must be >0 and <"+String.valueOf(SharedConstant.MAX_OBJECT_CHARS) +
	 														", and must match the java regex ->  " + SharedConstant.object_valid_regex + " | o:" +	objectName);
	 				 if (!file.exists()) 
	 					 throw new IllegalArgumentException("file does not exist -> " + file.getName());
	 				 
	 				 Path filePath = file.toPath();
	 				 if (!Files.isRegularFile(filePath))
	 				      throw new IllegalArgumentException("'" + file.getName() + "': not a regular file");
	 				 
	 				 Map<String, String> headerMap = new HashMap<>();
	 				 Map<String,String> queryParamMap = new HashMap<String,String>();

	 				 try {
	 					 headerMap.put("Content-Type", Optional.ofNullable(Files.probeContentType(filePath)).orElse(DEFAULT_CONTENT_TYPE));
	 				 } catch (IOException e) {
	 						throw new InternalCriticalException(e);
	 				 }
	 				 
	 				 long length = file.length();

	 				 queryParamMap.put("fileName", fileName);
	 				 
	 				 HttpResponse response = null;
	 				 RandomAccessFile raFile = null;
	 				 
	 				try { 
		 				try {
		 					raFile = new RandomAccessFile(filePath.toFile(), "r");
		 				} catch (FileNotFoundException e) {
		 						throw new InternalCriticalException(e);
		 				}
		 															
		 				response = executePost(API_OBJECT_UPLOAD, Optional.of(bucketName), Optional.of(objectName), headerMap, queryParamMap, raFile, (int) length, true);
	
		 				 try {
		 					 	String str = response.body().string();
		 					 	try {
		 							return this.objectMapper.readValue(str, ObjectMetadata.class);
		 							
		 						  } catch (JsonProcessingException e) {
 										throw new InternalCriticalException(e);
		 						  }
		 					  	
		 				 } catch (IOException e) {
		 						throw new InternalCriticalException(e);
		 				 }
		 				} finally {
		 					if (raFile!=null)
								try {
									raFile.close();
								} catch (IOException e) {
										throw new InternalCriticalException(e);
								}
		 				}
	 			 }
	 	  

 	  private static boolean isLinux() {
	 			if  (System.getenv("OS")!=null && System.getenv("OS").toLowerCase().contains("windows")) 
	 				return false;
	 			return true;
 		}

	 	 
	 			 
	  private String getCacheWorkDir() {									
		 		return getHomeDirAbsolutePath() + File.separator + "tmp" + File.separator +  rand.randomString(6);
	  }
		
	  private String getHomeDirAbsolutePath() {
				if (isLinux())
					return linux_home;
				return windows_home;
	  }


	private String getContentType(String src) {
	
		if (FSUtil.isPdf(src))
			return "application/pdf";
		
		if (FSUtil.isImage(src))  {
			String str = FilenameUtils.getExtension(src);
			if (str!=null && (str.toLowerCase().equals("jpg") ||  str.toLowerCase().equals("jpeg")))
				return "image/jpeg"; 
			return "image/"+str;
		}
		if (FSUtil.isVideo(src)) {
			return "video/"+FilenameUtils.getExtension(src);
		}
		if (FSUtil.isAudio(src))
			return "audio/"+FilenameUtils.getExtension(src);
		
		return DEFAULT_CONTENT_TYPE;
	 }
	
	
	/**
	 * @param endpoint
	 * @return {@code true} if the endpoint is valid {@link https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names}
	 * 
	 */
	private boolean isValidEndpoint(String endpoint) {
			
		  Check.requireNonNullStringArgument(endpoint, "endpoint is null or empty");
		  
		  if (InetAddressValidator.getInstance().isValid(endpoint)) {
		      return true;
		    }
		    // endpoint may be a hostname
		    // 
		    if (endpoint.length() < 1 || endpoint.length() > 253) {
		      return false;
		    }
	
		    for (String label : endpoint.split("\\.")) {
		      if (label.length() < 1 || label.length() > 63) {
		        return false;
		      }
	
		      if (!(label.matches(SharedConstant.valid_endpoint_regex))) {
		        return false;
		      }
		    }
		    return true;
	}




	
	
	

}



