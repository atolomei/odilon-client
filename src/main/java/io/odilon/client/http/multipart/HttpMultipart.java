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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLConnection;


/**
 * <p>HTTP Multipart request</p>
 */
public class HttpMultipart extends HttpRequest {

    private String boundary = "===" + System.currentTimeMillis() + "===";

    public HttpMultipart(String url, String credentials) {
    		this(url, credentials,null);
    }
    public HttpMultipart(String url, String credentials, ProgressListener listener) {
        super(url, credentials, listener);
    }

    @Override
    protected void write(HttpEntity entity) throws IOException {
        writeStart(entity);
        super.write(entity);
        writeEnd(entity);
    }

    @Override
    protected String getRequestMethod() {
        return "POST";
    }

    @Override
    protected boolean getDoOutput() {
        return true;
    }

    @Override
    protected String getContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }
    										
    protected void writeStart(HttpEntity requestEntity) throws IOException {
        String fieldName = "file";
        String fileName = requestEntity.getName();
        PrintWriter writer = getWriter();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"");
        writer.append(LINE_FEED);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName));
        writer.append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
    }

    protected void writeEnd(HttpEntity requestEntity) throws IOException {
        PrintWriter writer = getWriter();
        writer.flush();
        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.flush();
    }
}
