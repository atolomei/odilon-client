package io.odilon.client.search;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single search hit returned by the Odilon search API. It only contains
 * locating / display information, the object itself lives in Odilon storage.
 */
public class SearchResult implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonProperty("bucketName")
	public String bucketName;

	@JsonProperty("objectName")
	public String objectName;

	@JsonProperty("fileName")
	public String fileName;

	@JsonProperty("size")
	public long size;

	@JsonProperty("contentType")
	public String contentType;

	@JsonProperty("lastModified")
	public OffsetDateTime lastModified;

	@JsonProperty("etag")
	public String etag;

	@JsonProperty("tags")
	public List<String> tags = new ArrayList<String>();

	@JsonProperty("score")
	public float score;

	public SearchResult() {
	}

	@Override
	public String toString() {
		return "SearchResult [bucketName=" + bucketName + ", objectName=" + objectName + ", fileName=" + fileName + ", size=" + size + ", lastModified=" + lastModified + ", score=" + score + "]";
	}
}
