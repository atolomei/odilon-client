/*
 * Odilon Java SDK for Odilon Object Storage,
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import io.odilon.client.error.ODClientException;
import io.odilon.model.Bucket;
import io.odilon.model.MetricsValues;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SystemInfo;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;
import io.odilon.net.ErrorCode;
import io.odilon.net.ODHttpStatus;

/**
 * <p>{@code OdilonClient} contains the API to interact with an Odilon server.
 *</p>
 * <p>Implementation of this {@code Interface}: {@link ODClient}</p>
 * 
 * <b>Example</b>
 *  <p> This example connects to a Odilon server and sends a ping request to check the status of the server.</p>
 * <pre>{@code // these are the default values for the Server
 * String endpoint = "http://localhost";
 * int port = 9234;
 * String accessKey = "odilon";
 * String secretKey = "odilon";
 * 						
 * // OdilonClient is the interface, ODClient is the implementation
 * OdilonClient client = new ODClient(endpoint, port, accessKey, secretKey);
 *
 * // ping checks the status of server, it returns the String "ok" if the server is normal
 * 
 * String ping = client.ping();
 * if (!ping.equals("ok")) {
 *	System.out.println("ping error -> " + ping);
 *	System.exit(1);
 * }
 * }</pre>
 */
public interface OdilonClient {

	/* =======================================
	 * BUCKET
	 ==========================================*/

	/**
	 * <p>Creates a new {@link Bucket}.</p> 
	 * <p>
	 *  Odilon stores objects using a flat structure of containers called Buckets. 
	 *  A bucket is like a folder, it just contains binary objects, potentially a very large number. 
	 *  Every object contained by a bucket has a unique ObjectName in that bucket; therefore, the pair BucketName + ObjectName is 
	 *  a Unique ID for each object in Odilon.
	 *  </p>
	 * </p>The bucket must not exist, if it exists the method will throw a {@link ODClientException}.<br/>
	 *  bucketName length must be lower or equal to {@link io.odilon.model.SharedConstant#MAX_BUCKET_CHARS} and <br/> 
	 *  match the regular expression {@link io.odilon.model.SharedConstant#bucket_valid_regex SharedConstant.bucket_valid_regex} (see {@link isValidBucketName})
	 * </p>
	 * 
	 *  <b>Example:</b>
	 * <pre>{@code
	 * try {
	 *   String bucketName = "bucket-demo";
	 *   // check if the bucket exists, if not create it
	 *   if (client.existsBucket(bucketName))
	 *       System.out.println("bucket already exists ->" + bucketName );
	 *   else 
	 *       client.createBucket(bucketName);
	 * } catch (ODClientException e) {
	* 		System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	 * }
	 * }</pre>
	 * 
	 * @param bucketName Bucket name 
	 * @throws {@link ODClientException} if the bucket already exists (error code {@link io.odilon.net.ErrorCode#OBJECT_ALREADY_EXIST})
	 */
	public void createBucket(String bucketName) throws ODClientException;


	
	
	/**
	   * <p>Returns all buckets, sorted alphabetically</p>
	   * <p><b>Example:</b><br>
	   * <pre>{@code 
	   * OdilonClient odilonClient = new ODClient(url, port, accessKey, secretKey);
	   * List<Bucket> bucketList = odilonClient.listBuckets();
	   * bucketList.forEach( bucket ->  {
	   * 
	   * 	System.out.println(bucket.creationDate() + ", " + bucket.name());
	   * 
	   * 	if ( odilonClient.isEmpty(bucket.getName()) )
	   * 		System.out.println(bucket.name() + " -> is empty");
	   * 	else
	   * 		System.out.println(bucket.name() + " -> has objects");
	   * );
	   * }
	   * </pre>
	   * </p>
	   *
	   * @return List of Buckets, sorted alphabetically
	   * @throws {@link ODClientException}  
	   */
	public List<Bucket> listBuckets() throws ODClientException;
	
	
	
	
	/**
	 * <p>Returns {@code true} if the {@link String} objectName is a valid name for an Odilon object</p>
	 * <p>
	 * <ul>
	 * <li>objectName length less or equal to {@link io.odilon.model.SharedConstant#MMAX_OBJECT_CHARS}</li> 
	 * <li>objectName must match Java Regular expression {@link io.odilon.model.SharedConstant#object_valid_regex}
	 * </li>
	 * <ul>
	 * </p>
	 * <p>
	 * Informally speaking: object names can not have slash (/) or backslash (\)
	 * <br/><br/>
	 * Examples of invalid object names:<br/>
	 * <ul>
	 * <li>2023/11/349460828-7.2.a.3.implementacion.pdf</li>
	 * <li>2021/9/346049478-roses-2191636</li>
	 * </ul>
	 * <br/>
	 * Examples of valid object names:<br/><br/>
	 * <ul>
	 * <li>1-documento rio-90876</li>
	 * <li>2023-6-346227578-Quinquela-773x458.jpeg</li>
	 * <li>2023-11-349460978-compliance-basic.png.enc</li>
	 * </ul>
	 * </p>
	 * 
	 * @see {@link io.odilon.model.SharedConstant}
	 * 
	 * @param objectName name of the Object
	 * @return true if the objectName is a valid Odilon Object name
	 */
	public boolean isValidObjectName(String objectName);
	
	
	
	/**
	 * <p>Returns {@code true} if the {@link String} bucketName is a valid name for an Odilon Bucket</p>
	 * <p>
	 * <ul>
	 * <li>bucketName length less or equal to {@link io.odilon.model.SharedConstant#MAX_BUCKET_CHARS}</li> 
	 * <li>bucketName must match Java Regular expression {@link io.odilon.model.SharedConstant#bucket_valid_regex}
	 * <p>
	 * Informally speaking: letters A to Z and/or a to z, numbers, and/or - or _
	 * </p>
	 * </li>
	 * <ul>
	 * </p>
	 * 
	 * @see {@link io.odilon.model.SharedConstant}
	 * 
	 * @param bucketName name of the Bucket
	 * @return true if the bucketName is a valid Odilon Bucket name
	 */
	public boolean isValidBucketName(String bucketName);
	
	


	/**
	 * <p>Returns {@code true} if a bucket with name {@code bucketName} exists </p> 
	 * @param bucketName
	 * @return whether the bucket exists in the Server
	 * @throws {@link ODClientException}
	 */
	public boolean existsBucket(String bucketName) throws ODClientException;
	
	/**
	 * <p>Returns {@code true} if the bucket has no Objects, otherwise {@code false}</p>
	 * @param bucketName Bucket name
	 * @return true if the bucket has no Objects
	 * @throws {@link ODClientException} <br/> 
	 * BucketName is not an existing Bucket -> {@link ODHttpStatus.NOT_FOUND} with error code {@link ErrorCode.BUCKET_NOT_EXISTS} <br/>  
	 */
	public boolean isEmpty(String bucketName) throws ODClientException;

	
	
	/**
	 * <p>Returns the {@link Bucket}</p> 
	 * <p>It will throw a {@link ODClientException} if the bucket does not exist.</p>

	   * <p><b>Example:</b><br>
	   * <pre>{@code 
	   * 
	   * if (odilonClient.existsBucket("test-bucket")( {
	   * 	Bucket bucket = odilonClient.getBucket("test-bucket");
	   * 	System.out.println("bucket name ->" + bucket.getName() );
	   * 	System.out.println("bucket created ->" + bucket.getCreationDate().toString() );
	   * }
	   * }
	   * </pre>
	   * </p>
	 * 
	 * @param bucketName Bucket name
	 * @return Bucket
	 * @throws {@link ODClientException} <br/> 
	 * BucketName is not an existing Bucket -> {@link ODHttpStatus.NOT_FOUND} with error code {@link ErrorCode.BUCKET_NOT_EXISTS} <br/>  
	 */
	public Bucket getBucket(String bucketName) throws ODClientException;
	
	
	/**
	 * <p>Deletes a {@link Bucket}. The bucket must be empty to be deleted</p>
	 * <p><b>Example:</b><br>
	 * <pre>{@code try {
	 * // deletes all buckets that have no objects
	 * 		for (Bucket bucket:odilonClient.listBuckets()) {
     * 			if (odilonClient.isEmpty(bucket.getName())) {
     * 				odilonClient.deleteBucket(bucket.getName());
     * 			}
     * 		}
     *  } catch (ODClientException e) {
 	 * 		System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	 * }
	 * }
	 * </pre>
	 * </p>
	 * @param bucketName Bucket name
	 * @throws {@link ODClientException} <br/>
	 * Bucket does not exist -> {@link ODHttpStatus.NOT_FOUND} with error code {@link ErrorCode.BUCKET_NOT_EXISTS} <br/>
	 * Bucket is not empty -> {@link ODHttpStatus.CONFLICT with error code {@link ErrorCode.BUCKET_NOT_EMPTY}
	 */
	public void deleteBucket(String bucketName) throws ODClientException;

	/**
	* <p>Deletes all the previous versions of all the Objects in the {@link Bucket}.<br/> 
	*  Note that it does not delete the current version of the objects (called <b>head version</b>). <br/>
	* </p>
	* <p>This method is sometimes used to free disk in the server</p>
	* <b>Async execution</b>
	* <p>This method returns immediately after sending the command to the server, which processes the task asynchronously.
	* The async command is atomic on each object but not globally, which means that until it finishes some objects can still have versions.</p>
 	* <p><b>Example:</b>
	* <pre>{@code
	* try {
	*       Bucket bucket = odilonClient.getBucket("test-version-control");

	*       odilonClient.deleteAllBucketVersions(bucket.getName());
	*       
	* } catch (ODClientException e) {
	* 		System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	* }
	* </pre>
	* </p>	 
	* @param bucketName Bucket name
	* @throws {@link ODClientException} <br/>
	* Bucket does not exist -> {@link ODHttpStatus.NOT_FOUND} with error code {@link ErrorCode.BUCKET_NOT_EXISTS} <br/>
	* Server does not have Version Control enabled -> {@link ODHttpStatus.METHOD_NOT_ALLOWED} with error code {@link ErrorCode.API_NOT_ENABLED} Version Control not enabled
	*/
	public void deleteAllBucketVersions(String bucketName) throws ODClientException;
	
	/* =======================================
	 * OBJECT
	 ==========================================*/

	/* -----------------------
	 * OBJECT get   	  
	   ------------------------*/
	
	/**
	 * <p>Checks for the existence of an Object.</p>
	 * <p>Note that if the {@link Bucket} does not exist, the method does not throw an {@code Exception}, it returns {@code false}</p>
	 * 
	 * @param bucketName Bucket name of the Bucket
	 * @param objectName name if object
	 * @return true if the Object exist
	 * @throws {@link ODClientException}
	 * @throws IOException
	 */
	public boolean existsObject(String bucketName, String objectName) throws ODClientException, IOException;
	
	

	/**
	 * <p>Returns the {@link ObjectMetadata} of the Object. <br/>
	 * In addition to the binary file, an Object has metadata (called {@link ObjectMetadata}) 
	 * that is returned by some of the API calls. Odilon allows to retrieve Objects individually 
	 * by <b>bucketName</b> + <b>objectName</b> and also supports to list the contents of a bucket and other simple queries.
	 *</p>
	 * <p>Every object contained by a bucket has a unique objectName in that bucket; 
	 * therefore, the pair <b>bucketName</b> + <b>objectName</b> is a Global Unique Identifier for each object in Odilon.
	 * </p>
 	* <p><b>Example:</b>
	* <pre>{@code try {
	*       Bucket bucket = odilonClient.getBucket("test-version-control");
	*       
	*       ObjectMetadata meta = odilonClient.getObjectMetadata(bucket.getName(), "test1");
	*       
	*       System.out.println(meta.toString());
	*       
	* } catch (ODClientException e) {
	* 	System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	* 	System.exit(1);
	* }
	* </pre>
	* </p>	 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata getObjectMetadata(String bucketName, String objectName) throws ODClientException;
	

	/**
	 * <p>Returns the binary data (File) of this Object</p> 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * 
	 * @throws {@link ODClientException}
	 */
	public InputStream getObject(String bucketName, String objectName) throws ODClientException;
	
	/**
	 * <p>Retrieves the binary data of this Object and saves it to a {@link File} with path {@code filePath}</p>
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param filePath path and file name where to save the data downloaded (example: "c:\temp\myfile.pdf")
	 * 
	 * @throws 
	 * 			if object does not exist -> {@link ObjectNotFoundException} ODHttpStatus.NOT_FOUND, ErrorCode.OBJECT_NOT_FOUND
	 * 
	 * @throws IOException
	 */
	public void getObject(String bucketName, String objectName, String filePath) throws ODClientException, IOException;
	
	
	/**
	 * <p>Returns a temporary URL to access or download the binary data of an Object without authentication</p>
	 * <p>A presigned URL is a way to grant temporary access to an Object, for example in an HTML webpage.
	 *  It remains valid for a limited period of time which is specified when the URL is generated.
	 * </p>
	 * <p>For the following example see also {@link #listObjects}</p>
	 *<b>Example</b>:
	 *<pre>
	 *{@code 
	 * this.bucket = getClient().listBuckets().get(0);
	 *		
	 *		 ResultSet<Item<ObjectMetadata>> rs = getClient().listObjects(this.bucket.getName());
	 *		 int counter = 0;
	 *		 while (rs.hasNext() && counter++ < MAX) {
	 *			 Item<ObjectMetadata> item = rs.next();
	 *			 if (item.isOk()) {
	 *				 	ObjectMetadata meta = item.getObject();
	 * 					
	 * 					// by default the link lasts 7 days
	 *					logger.debug(meta.bucketName + " / " + meta.objectName + " (7 days) -> " + getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName));	 
	 *					
	 *					// url valid for 5 minutes 
	 *					logger.debug(meta.bucketName + " / " + meta.objectName + " (5 min) -> " + getClient().getPresignedObjectUrl(meta.bucketName, meta.objectName, Optional<Integer>(Integer.valueOf(60*5)));	 
	 *			 }
	 *		 }
	 *	}
	 * </pre>
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param expiresInSeconds duration in seconds for the url to be valid
	 * @return temporary url to download the file without authentication
	 * 
	 * @throws {@link ODClientException}
	 */
	
	
	public String getPresignedObjectUrl(String bucketName, String objectName, Optional<Integer> expiresInSeconds) throws ODClientException;

	
	/**
	 * <p>Returns a temporary URL to access or download the binary data of an Object without authentication</p>
	 * <p>This is method calls {@link #getPresignedObjectUrl} with the default value for  {@code expiresInSeconds} (ie. 7 days in seconds)</p>
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @return
	 * @throws {@link ODClientException}
	 */
	public String getPresignedObjectUrl(String bucketName, String objectName) throws ODClientException;
	
	
	/* --------------------
	/* OBJECT put   	  
	  --------------------*/
	
	/**
	 * <p>Uploads a File or any other binary stream to the server. It will create a new object or update an existing one.</p>
	 * <p>Odilon stores objects using a flat structure of containers called Buckets. <br/>
	 * A bucket is like a folder, it just contains binary objects, potentially a very large number. <br/> 
	 * Every object contained by a bucket has a unique ObjectName in that bucket; therefore, the pair <b>BucketName</b> + <b>ObjectName</b> 
	 * is a Unique ID for each object in Odilon.</p>
	 * <p>Uploading a File requires the Bucket to exist and the ObjectName to be unique for that bucket.</p>
	 * <p>If the objectName does not exist in the bucket, the server will create a new object, if
	 * the objectName exists in the bucket the server will update the object with the new binary data. 
	 * in this case, if the server has Version Control enabled it will make a new version and the previous version of the object will not be deleted.
	 *  If the server does not have Version Control enabled, the former version will be discarded.
	 * </p>
	 * <p>The Odilon client closes the {@link InputStream} after sending the data to the server. 
	 *  However, if an {@link Exception} other than {@link ODClientException} is thrown by this method, the
	 * {@link InputStream} may not have been closed.</p> 
	 * <p>Therefore callers must always attempt to close the {@link Inputstream}</p>
	 * 
	 * <b>Example:</b>
	 * <pre>{@code
	 * File file = new File("test.pdf");
	 *	String bucketName = "bucket-demo";
	 *	String objectName = file.getName();
	 *
	 * try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))){	
	 *		client.putObjectStream(bucketName,objectName,inputStream,Optional.of(file.getName()),Optional.empty());
	 *	} catch (ODClientException e) {
	 * 			System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	 *	} catch (FileNotFoundException | IOException e1) {
	 *		System.out.println(e1.getMessage());
	 *	}
	 * }
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param stream 
	 * @param fileName
	 * @param contentType
	 * @return {@link ObjectMetadata} of the Object created or updated
	 * 
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, String fileName, String contentType) throws ODClientException;
		
	/**
	 * <p>Uploads a File or any other binary stream to the server. It will create a new object or update an existing one.</p> 
	 * <p>This method does the same as {@link #putObject} <br/>
	 * but with a local {@link File} instead of an {@link InputStream}</p>
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param file
	 * @return {@link ObjectMetadata} of the Object created or updated
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata putObject(String bucketName, String objectName, File file) throws ODClientException;
	
	
	/**
	 * <p>Uploads a File or any other binary stream to the server. It will create a new object or update an existing one.</p>
	 * <p>This method calls {@link #putObject} 
	 * </p>
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param stream InputStream with the binary data to upload
	 * @param fileName name of the File uploaded
	 * @return the {@link ObjectMetadata} of the Object
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, String fileName) throws ODClientException;
	
	/**
	 * <p>Uploads a File or any other binary stream to the server. It will create a new object or update an existing one.</p>	  
	 * <p>This method calls {@link #putObject}</p>
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param stream
	 * @param fileName
	 * @param size
	 * @param contentType
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, Optional<String> fileName, Optional<Long> size, Optional<String> contentType) throws ODClientException;

	
	/**
	 * <p>Uploads a File or any other binary stream to the server. It will create a new object or update an existing one.</p>
	 * <p>This method calls {@link #putObject}</p>
	 * 
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @param stream
	 * @param fileName
	 * @param size
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, Optional<String> fileName, Optional<Long> size) throws ODClientException;
	
	
	/* -----------------------
	 * OBJECT delete   	     	  
	 ------------------------*/
	
	/**
	 *<p> Removes an Object from a {@link Bucket}.</p>
	 *
	 * <p><b>Example:</b><br>
	 * <pre>{@code odilonClient.deleteObject("my-bucketname", "my-objectname"); }</pre>
	 * </p>
	 *
	 * @param bucketName Bucket name
	 * @param objectName Object name in the bucket
	 * @throws {@link ODClientException}
	 */
	public void deleteObject(String bucketName, String objectName) throws ODClientException;
	
	
	/* -----------------------
	 * OBJECT version control   	     	  
	  ------------------------*/
	/**
	 * <p>Returns {@code true} if the Object has previous versions, ie. at least there is one version older than the <b>head version</b> (in which case the head version must be greater than 0)</p>
	 * @param bucketName Bucket name
	 * @param objectName Object name
	 * @return {@code true} if the Object has more versions than the head version (ie. the head version must be greater than 0)
	 * @throws {@link ODClientException}
	 * @throws IOException
	 */
	public boolean hasVersions(String bucketName, String objectName) throws ODClientException, IOException;
	
	/**
	 * <p>Returns {@link ObjectMetadata} of the version immediately previous to the <b>head version</b>, or {@code null} if there is no previous version</p>
	 * 
	 * @param bucketName
	 * @param objectName
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ObjectMetadata getObjectMetadataPreviousVersion(String bucketName, String objectName) throws ODClientException;
	
	
	
	/**
	 * <p>Restores the previous version of the Object and deletes current version. </p>
	 * <p>If the object does not have any previous versions, it throws an {@link ODClientException}</p>
	 * 
	 * @param bucketName
	 * @param objectName
	 * @throws {@link ODClientException} <br/>
	 * <ul>
	 * <li>The server does not have Version Control enabled -> {@link OdilonServerAPIException} ODHttpStatus.METHOD_NOT_ALLOWED, ErrorCode.API_NOT_ENABLED <br/><br/></li>
	 * <li>The Object does not have a previous version (i.e. current version is version 0) -> {@link ObjectNotFoundException} ODHttpStatus.NOT_FOUND, ErrorCode.OBJECT_NOT_FOUND <br/><br/></li>
	 * <li>All the object's previous version has been deleted via {@link deleteAllBucketVersions} or {@link deleteObjectAllVersions} -> {@link ObjectNotFoundException} ODHttpStatus.NOT_FOUND, ErrorCode.OBJECT_NOT_FOUND <br/><br/></li> 
	 * <li>Other, unexpected causes -> ODHttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR<br/><br/></li>
	 * </ul>
	 * 
	 */
	public void restoreObjectPreviousVersions(String bucketName, String objectName) throws ODClientException;

	
	/**
	 * <p>Returns all the previous versions of an Object, the current version (head version) is not included in the List returned</p>
	 * @param bucketName
	 * @param objectName
	 * @return list of {@link ObjectMetadata} sorted by version number, from 0 to (head version - 1)
	 * @throws {@link ODClientException}
	 */
	public List<ObjectMetadata> getObjectMetadataPreviousVersionAll(String bucketName, String objectName) throws ODClientException;	
	
	
	/**
	 * <p>Returns the binary data of the object's previous version or {@code null} if there is no previous version.</p>
	 * @param bucketName
	 * @param objectName
	 * @return ObjectMetadata of the object's previous version
	 * @throws {@link ODClientException}
	 */
	public InputStream getObjectPreviousVersion(String bucketName, String objectName) throws ODClientException;

	
	/**
	 * <p>Returns the binary data of the version #{@code version} of the object's or {@code null} if the object does not have a version #{@code version}.</p> 
	 * @param bucketName
	 * @param objectName
	 * @param version
	 * @return
	 * @throws {@link ODClientException}
	 */
	public InputStream getObjectVersion(String bucketName, String objectName, int version) throws ODClientException;
	
	
	/**
	 * <p>Deletes all the Object's  previous versions, it does not delete the object current version (<b>head version</b>).</p> 
	 * <p>After calling this method the Object will have only the <b>head version</b>, 
	 * and trying to retrieve a previous version will throw {@link ObjectNotFoundException}.</p>
	 * 
	 * @param bucketName
	 * @param objectName
	 * @throws {@link ODClientException}
	 */
	public void deleteObjectAllVersions(String bucketName, String objectName) throws ODClientException;
	 

	/* =======================================
	 * 	QUERY
	 ==========================================*/

	/**
	 * <p>Lists the objects in the {@link Bucket}. </p>
	 * <p>ResultSet</p> 
	 * {@link Item Item &ltObjectMetadata&gt} can contain:<br/>
	 * it item.isOk() ->  a <b>{@code ObjectMetadata}</b> .<br/>
	 * otherwise -> or an String with an error
	 * </p>
	 * 
	 * <p>If prefix is not {@code Optional.empty()} the list will include only objects where the objectName starts with the prefix. <br/>
	 * {@code pageSize} is the number of items retrieved by each call to the server, if the value is {@code Optional.empty()} the client will use the default pageSize value.<br/>
	 * </p> 

	 * <p>Example list all bucket's objects:
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
	* 		System.out.println( "HTTP status -> " + String.valueOf(e.getHttpStatus())+ " | ErrorMsg -> " + e.getMessage() + " Odilon Error Code -> " + String.valueOf(e.getErrorCode()) );
	 *	}
	 *</p>
	 *}
	 * </pre>
	 */
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName, Optional<String> prefix, Optional<Integer> pageSize) throws ODClientException;
	
	/**
	 *<p>Lists the objects in the {@link Bucket}.</p>
	 *<p> see {@link #listObjects}</p>
	 * 
 	 * @param bucketName Bucket name
	 * @param prefix
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName, String prefix) throws ODClientException;	
	
	
	/**
	 *<p>Lists the objects in the {@link Bucket}.</p> 
	 *<p> see {@link #listObjects}</p>
	 * 
 	 * @param bucketName name of the bucket
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ResultSet<Item<ObjectMetadata>> listObjects(String bucketName) throws ODClientException;
	
	
	/**
	 *<p>Lists the objects in the {@link Bucket}.</p> 
	 *<p> see {@link #listObjects}</p>
	 * 
 	 * @param bucketName name of the bucket
	 * @return
	 * @throws {@link ODClientException}
	 */
	public ResultSet<Item<ObjectMetadata>> listObjects(Bucket bucket) throws ODClientException;
	
	
	/* =======================================
	 * MONITOR
	 ==========================================*/
	  /**
	   * <p>Returns the String <b>"ok"</b> if the server is normal or a {@code String} with the error reported by the Server. <br/>
	   * If the client can not connect to the Server, the method returns a message <b>"can not connect"</b></p>
	   * </p>
	   * <b>Example:</b>
	   * <pre>{@code 
	   * String endpoint = "http://localhost";
	   * int port = 9234;
	   * String accessKey = "odilon";
	   * String secretKey = "odilon";
	   * 
	   * OdilonClient client = new ODClient(endpoint, port, accessKey, secretKey);
	   * 
	   * String pingResult = odilonClient.ping();
	   * 
	   * if (!pingResult.equals("ok")) {
	   *   System.out.println( "Server error -> " + pingResult));
	   * }</pre>
	  * 
	  * @return String "ok" or the error reported by the Server.
	  */
	public String ping();
	
/**
 * 
 * <p>Returns an instance of {@link MetricsValues} with info related to the activity of the server</p>
 * <p>They are counters of events and meters that measure events per second in a 1m 5m and 15m windows
 * <ul>
 * <li><b>Counter</b> 
 * A counter is just a gauge for an AtomicLong instance. You can increment or decrement its value. 
 *<br/>
 *</li>
 * <li><b>Meter</b>
 * A meter measures the rate of events over time (e.g., “requests per second”). In addition to the mean rate, meters also track 1-, 5-, and 15-minute moving averages.
 * <br/>
 * </li>
 *<br/>
 * </ul>
 * See <a href="https://metrics.dropwizard.io/4.2.0/" target="_blank"/>https://metrics.dropwizard.io/4.2.0</a>
 * </p>
 * <b>Example</b>: <br/> <br/>
 *   cacheObjectHitCounter -> 1,224 <br/>   
 *   cacheObjectMissCounter -> 637  <br/>
 *   cacheObjectSize -> 475 <br/>
 *   decryptFileMeter -> 0.27 0.42 0.24 <br/>
 *   encrpytFileMeter -> 0.49 0.67 0.46  <br/>
 *   objectCreateCounter -> 475 <br/>
 *   objectDeleteCounter -> 0 <br/>
 *   objectDeleteVersionCounter -> 0 <br/>
 *   objectGetMeter -> 0.55 0.85 0.48 <br/>
 *   objectPutMeter -> 0.49 0.67 0.46 <br/>
 *   objectUpdateCounter -> 162 <br/>
 *   replicaObjectCreate -> 0 <br/>
 *   replicaObjectDelete -> 0 <br/>
 *   replicaObjectUpdate -> 0 <br/>
 *   vaultDecryptMeter -> 0.00 0.00 0.00 <br/>
 *   vaultEncryptMeter -> 0.00 0.00 0.00 <br/>
 </p> 
 * 
 * @return {@link MetricsValues} 
 * @throws {@link ODClientException}
 */
	public MetricsValues metrics() throws ODClientException;
	
	/**
	 * <p>Returns an instance of {@link SystemInfo} with the info of the settings of the server</p>
	 *  
	 *  
	 * @return {@link SystemInfo}
	 * @throws {@link ODClientException}
 	*/
	public SystemInfo systemInfo() throws ODClientException;

	
	/**
	 * <p>Checks if the server has version control enabled</p>
	 * 
	 * @return true if the server has version control enabled
	 * @throws {@link ODClientException}
	 */
	public boolean isVersionControl() throws ODClientException;
	
	/**
	 * <p>Returns the url of the Odilon server</p>
	 * @return server url (without the port)
	 */
	public String getUrl();

	/* =======================================
	 * CLIENT SETTINGS
	 ==========================================*/
	
	  /**
	   * <p>Sets HTTP connect, write and read timeouts.  A value of 0 means no timeout, otherwise values must be between 1 and
	   * Integer.MAX_VALUE when converted to milliseconds.</p>
	   * <b>Example:</b><br>
	   * <pre>{@code odilonClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
	   *                            TimeUnit.SECONDS.toMillis(30)); }</pre>
	   *                            
	   * @param connectTimeout    HTTP connect timeout in milliseconds.
	   * @param writeTimeout      HTTP write timeout in milliseconds.
	   * @param readTimeout       HTTP read timeout in milliseconds.
	   */

	
	public void setTimeout(long connectTimeoutMilliseconds, long writeTimeoutMilliseconds, long readTimeoutMilliseconds);
	
	public void setChunkSize(int chunkSize);
	public int getChunkSize();
	
	
	/* =======================================
	 * DEBUG
	 ==========================================*/
	
	/**
	 * <p>enabled printing the request-response raw info to a stream</p>
	 */
	public void traceOn(OutputStream traceStream);
	
	/**
	 * <p>disable printing the request-response raw info to a stream</p>
	 */
	public void traceOff() throws IOException;










	
	
	
	
	
	
}








/*
 *  
	// public ObjectMetadata putObjectStream(String bucketName, String objectName, InputStream stream, String fileName, String contentType) throws ODClientException;

*
 *   
 * <h2>Bucket</h2>
 * <p>Main methods are:</p>
 * <ul>
 * <li>Creating a bucket</li>
 * <li>Delete bucket</li>
 * <li>get a bucket</li>
 * <li>Listing buckets</li>
 * <li>check if a bucket exist</li>
 * <li>Delete previous versions for all the bucket's objects</li>
 * </ul>
 * <h2>Object</h2>
 * 
 * <h3>Object Get</h3>
 * <ul>
 * <li>Removing an object</li>
 * <li>Read object Metadata</li>
 * <li>Reading an object</li>
 * <li>Deleting an object</li>
 * </ul>
 * 
 * <h3>Object Put</h3>
 * <ul>
 * <li>Removing an object</li>
 * <li>Read object Metadata</li>
 * <li>Reading an object</li>
 * <li>Deleting an object</li>
 * </ul>
 * 
 * <h3>Object Delete</h3>
 *  <ul>
 * <li>Removing an object</li>
 * <li>Read object Metadata</li>
 * <li>Reading an object</li>
 * <li>Deleting an object</li>
 * </ul>
 *  
 * <h3>Object Versioning</h3>
 * <ul>
 * <li>Removing an object</li>
 * <li>Read object Metadata</li>
 * <li>Reading an object</li>
 * <li>Deleting an object</li>
 * </ul>
*/

