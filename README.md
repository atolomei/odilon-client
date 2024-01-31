
![sunflower-g72ba79a53_1280](https://github.com/atolomei/odilon-client/assets/29349757/13cf02bd-68df-41f4-ba09-c26519c287c1)

<h1>Odilon Java SDK </h1>

<h2>About Odilon</h2>
<p> <a href="https://odilon.io">Odilon</a> is a scalable and lightweight Open Source Object Storage that runs on standard hardware.</p>
<p>It is an infrastructure software designed to be used by applications that need to store to store terabytes of medium to large size objects (like photos, pdfs, audio, video) securely and safely through encryption, replication and redundancy. </p> 
<p>It has a simple single-level folder structure similar to the Bucket / Object model of Amazon S3. It is small and easy to integrate, offers encryption, data protection and fault tolerance (software RAID and Erasure Codes) and detection of silent data degradation. Odilon also supports version control and master - standby replication over the Internet for disaster recovery and ransomware recovery.</p>
<p>
For more info visit Odilon's website <a href="https://odilon.io/development.html">Java Development with Odilon SDK</a> and also <a href="https://githug.com/odilon-server.html"> GitHub page</a> 	
</p>

<h2>Odilon Java SDK Concepts</h2>

<p>A Java client program that interacts with the Odilon server must include the Odilon SDK jar in the classpath.
A typical architecture for a Web Application is</p>
<br/>
<br/>

![web-app-odilon-en](https://github.com/atolomei/odilon-client/assets/29349757/aa736909-f247-4a18-99b9-166adacf0929)

<br/>
<br/>
<p>In order to access the Odilon server from a Java Application you have to include Odilon client JAR in the classpath. The interaction is managed by an instance of <b>OdilonClient</b> that connects to the server using the credentials: <b>AccessKey</b> (ie. username) and <b>SecretKey</b> (ie. password)</p>
<br/>
<br/>


```java
/* these are the default values for the Server */
String endpoint = "http://localhost";
int port = 9234;
String accessKey = "odilon";
String secretKey = "odilon";
						
/** OdilonClient is the interface, ODClient is the implementation */
OdilonClient client = new ODClient(endpoint, port, accessKey, secretKey);

/** ping checks the status of server, it returns the String "ok" when the server is normal */
String ping = client.ping();
if (!ping.equals("ok")) {
	System.out.println("ping error -> " + ping);
	System.exit(1);
}
```
<br/>
<br/>
<p>Odilon stores objects using a flat structure of containers called Buckets. A bucket is like a folder, it just contains binary objects, potentially a very large number. Every object contained by a bucket has a unique ObjectName in that bucket; therefore, the pair BucketName + ObjectName is a Unique ID for each object in Odilon.</p>
<br/>
<br/>

```java
try {
    String bucketName = "bucket-demo";
    /** check if the bucket exists, if not create it */
    if (client.existsBucket(bucketName))
        System.out.println("bucket already exists ->" + bucketName );
    else 
        client.createBucket(bucketName);
} catch (ODClientException e) {
        System.out.println(String.valueOf(e.getHttpStatus())+ " " + e.getMessage()+" " + String.valueOf(e.getErrorCode()));
}
```

<br/>
<br/>
<p>Uploading a File requires the Bucket to exist and the ObjectName to be unique for that bucket.</p>
<br/>
<br/>

```java
File file = new File("test.pdf");
String bucketName = "bucket-demo";
String objectName = file.getName();

try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {	
	client.putObjectStream(bucketName, objectName, inputStream, Optional.of(file.getName()), Optional.empty());
} catch (ODClientException e) {
	System.out.println(String.valueOf(e.getHttpStatus())+" " + e.getMessage()+" " + String.valueOf(e.getErrorCode()));
} catch (FileNotFoundException | IOException e1) {
	System.out.println(e1.getClass().getName() + " " + e1.getMessage());
}
```

<br/>
<br/>
<p>In addition to the binary file, an Object has Metadata (called ObjectMetadata) that is returned by some of the API calls. Odilon allows to retrieve Objects individually by BucketName + ObjectName and also supports to list the contents of a bucket and other simple queries.</p>

<br/>
<br/>

```java
try {
	/** list all bucket's objects */
	ResultSet<Item <ObjectMetadata>> resultSet = client.listObjects(bucket.getName());
	while (resultSet.hasNext()) {
		Item item = resultSet.next();
		if (item.isOk())
			System.out.println("ObjectName:"+item.getObject().objectName+" | file: " + item.getObject().fileName);
		else
			System.out.println(item.getErrorString());
	}
} catch (ODClientException e) {
   	System.out.println(String.valueOf( e.getHttpStatus())+ " "+e.getMessage() + " "+String.valueOf(e.getErrorCode()));
}
```
<br/>
<br/>

<h2>Sample Programs</h2>



<ul>
<li><a href="https://github.com/atolomei/odilon-client/blob/main/src/test/io/odilon/demo/SampleBucketCreation.java">Create Bucket</a></li>	
<li><a href="https://github.com/atolomei/odilon-client/blob/main/src/test/io/odilon/demo/SampleListBuckets.java">List Buckets</a></li>	
<li><a href="https://github.com/atolomei/odilon-client/blob/main/src/test/io/odilon/demo/SamplePutObject.java">Upload file</a></li>		
<li><a href="https://github.com/atolomei/odilon-client/blob/main/src/test/io/odilon/demo/SampleListObjects.java">List all objects in a Bucket</a></li>	
<li><a href="https://odilon.io/examples/SamplePresignedUrl.java">Get presigned urls of Objects</a></li>	
<li><a href="https://github.com/atolomei/miniomigration">Minio to Odilon migration</a></li>
</ul>

<h2>Resources</h2>
<p>
<ul>
<li><a href="https://odilon.io" target="_blank">Odilon website</a></li>	
<li><a href="https://odilon.io/configuration-linux.html" target="_blank">Installation, Configuration and Operation on Linux</a></li>	
<li><a href="https://odilon.io/configuration-windows.html" target="_blank">Installation, Configuration and Operation on Windows</a></li>		
<li><a href="https://odilon.io/development.html" target="_blank">Java Application Development with Odilon</a></li>	
<li><a href="https://odilon.io/javadoc/index.html" target="_blank">Odilon SDK Javadoc</a></li>	
<li><a href="https://twitter.com/odilonSoftware" target="_blank">Twitter</a></li>
</ul>
</p>


<h2>Odilon Server</h2>
<p>See <a href="https://github.com/atolomei/odilon-server" target="_blank">odilon Server</a>
</p>

