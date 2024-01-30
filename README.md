<h1>Odilon Java SDK </h1>

<h2>About Odilon</h2>
<p>Odilon is a scalable and lightweight Open Source Object Storage that runs on standard hardware.</p>
<p>It is an infrastructure software designed to be used by applications that need to store to store terabytes of medium to large size objects (like photos, pdfs, audio, video) securely and safely through encryption, replication and redundancy. </p> 
<p>It has a simple single-level folder structure similar to the Bucket / Object model of Amazon S3. It is small and easy to integrate, offers encryption, data protection and fault tolerance (software RAID and Erasure Codes) and detection of silent data degradation. Odilon also supports version control and master - standby replication over the Internet for disaster recovery and ransomware recovery.</p>
<p>
For more info visit Odilon's website <a href="https://odilon.io/development.html">Java Development with Odilon SDK</a> and also <a href="https://githug.com/odilon-server.html"> GitHub page</a> 	
</p>

<h2>Main concepts</h2>
<p>In order to access the Odilon server from a Java Application you have to include Odilon client JAR in the classpath. The interaction is managed by an instance of OdilonClient that connects to the server using the credentials: AccessKey (ie. username) and SecretKey (ie. password)</p>
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




<h2>Odilon Server</h2>
<p>See <a href="https://github.com/atolomei/odilon-client" target="_blank">odilon Server</a>
</p>

