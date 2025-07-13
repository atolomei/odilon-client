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
package io.odilon.client.http;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import io.odilon.log.Logger;

/**
 * <p>
 * HTTP response header class
 * </p>
 *
 * @author atolomei@novamens.com (Alejandro Tolomei)
 *
 */

public class ResponseHeader {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ResponseHeader.class.getName());

    static final DateTimeFormatter http_date = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Header("Content-Length")
    private long contentLength;
    @Header("Content-Type")
    private String contentType;
    @Header("Date")
    private OffsetDateTime date;
    @Header("ETag")
    private String etag;
    @Header("Last-Modified")
    private OffsetDateTime lastModified;
    @Header("Server")
    private String server;
    @Header("Status Code")
    private String statusCode;
    @Header("Transfer-Encoding")
    private String transferEncoding;

    /**
     * <p>
     * Sets content length
     * </p>
     */
    public void setContentLength(String contentLength) {
        this.contentLength = Long.parseLong(contentLength);
    }

    public long contentLength() {
        return this.contentLength;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() {
        return this.contentType;
    }

    public void setDate(String strdate) {
        this.date = OffsetDateTime.from(http_date.parse(strdate));
    }

    public OffsetDateTime date() {
        return this.date;
    }

    public void setEtag(String etag) {
        this.etag = etag.replaceAll("\"", "");
    }

    public String etag() {
        return this.etag;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = OffsetDateTime.from(http_date.parse(lastModified));
    }

    public OffsetDateTime lastModified() {
        return this.lastModified;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String server() {
        return this.server;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String statusCode() {
        return this.statusCode;
    }

    public void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }

    public String transferEncoding() {
        return this.transferEncoding;
    }

    public String toJSON() {
        StringBuilder str = new StringBuilder();
        str.append("\"contentLength\":" + String.valueOf(contentLength));
        str.append("\"contentType\":" + (Optional.ofNullable(contentType).isPresent() ? ("\"" + contentType + "\"") : "null"));
        str.append("\"date\":" + (Optional.ofNullable(date).isPresent() ? ("\"" + date.toString() + "\"") : "null"));
        str.append("\"etag\":" + (Optional.ofNullable(etag).isPresent() ? ("\"" + etag + "\"") : "null"));
        str.append("\"lastModified\":"
                + (Optional.ofNullable(lastModified).isPresent() ? ("\"" + lastModified.toString() + "\"") : "null"));
        str.append("\"server\":" + (Optional.ofNullable(server).isPresent() ? ("\"" + server + "\"") : "null"));
        str.append("\"statusCode\":" + (Optional.ofNullable(statusCode).isPresent() ? ("\"" + statusCode + "\"") : "null"));
        str.append("\"transferEncoding\":"
                + (Optional.ofNullable(transferEncoding).isPresent() ? ("\"" + transferEncoding + "\"") : "null"));
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getSimpleName() + "{");
        str.append(toJSON());
        str.append("}");
        return str.toString();
    }

}
