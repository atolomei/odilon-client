package io.odilon.client.search;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status snapshot of the server's search index.
 */
public class IndexStatus implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String READY = "READY";
	public static final String REBUILDING = "REBUILDING";
	public static final String DEGRADED = "DEGRADED";
	public static final String DISABLED = "DISABLED";

	@JsonProperty("status")
	public String status;

	@JsonProperty("documents")
	public long documents;

	@JsonProperty("lastIndexed")
	public OffsetDateTime lastIndexed;

	@JsonProperty("lastRebuild")
	public OffsetDateTime lastRebuild;

	@JsonProperty("pending")
	public int pending;

	@JsonProperty("errors")
	public long errors;

	public IndexStatus() {
	}

	@Override
	public String toString() {
		return "IndexStatus [status=" + status + ", documents=" + documents + ", lastIndexed=" + lastIndexed + ", lastRebuild=" + lastRebuild + ", pending=" + pending + ", errors=" + errors + "]";
	}
}
