package io.odilon.client.search;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Search parameters for {@code OdilonClient.search(SearchQuery)}. All fields are
 * optional, non-null fields are combined with AND semantics on the server.
 * <ul>
 * <li>{@code bucketName} exact bucket filter</li>
 * <li>{@code q} free text over the catch-all field</li>
 * <li>{@code objectName} tokenized, case-insensitive match on the object key</li>
 * <li>{@code fileName} tokenized, case-insensitive match on the file name
 * (supports Lucene wildcards * and ?)</li>
 * <li>{@code lastModifiedFrom} / {@code lastModifiedTo} inclusive date range</li>
 * <li>{@code metadata} exact-match filters on custom metadata keys</li>
 * <li>{@code offset} / {@code maxResults} pagination (server caps at 1000)</li>
 * </ul>
 */
public class SearchQuery implements Serializable {

	private static final long serialVersionUID = 1L;

	public String bucketName;
	public String q;
	public String objectName;
	public String fileName;
	public OffsetDateTime lastModifiedFrom;
	public OffsetDateTime lastModifiedTo;
	public Map<String, String> metadata = new LinkedHashMap<String, String>();
	public int offset = 0;
	public int maxResults = 100;

	public SearchQuery() {
	}

	public SearchQuery bucketName(String v) {
		this.bucketName = v;
		return this;
	}

	public SearchQuery q(String v) {
		this.q = v;
		return this;
	}

	public SearchQuery objectName(String v) {
		this.objectName = v;
		return this;
	}

	public SearchQuery fileName(String v) {
		this.fileName = v;
		return this;
	}

	public SearchQuery lastModifiedFrom(OffsetDateTime v) {
		this.lastModifiedFrom = v;
		return this;
	}

	public SearchQuery lastModifiedTo(OffsetDateTime v) {
		this.lastModifiedTo = v;
		return this;
	}

	public SearchQuery meta(String key, String value) {
		this.metadata.put(key, value);
		return this;
	}

	public SearchQuery offset(int v) {
		this.offset = v;
		return this;
	}

	public SearchQuery maxResults(int v) {
		this.maxResults = v;
		return this;
	}
}
