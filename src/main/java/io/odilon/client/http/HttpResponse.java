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

import java.util.List;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * <p>
 * {@link ResponseHeader} and {@link Response} into one object to pass/return in
 * various methods
 * </p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public class HttpResponse {

    private ResponseHeader header;
    private Response response;

    public HttpResponse(ResponseHeader header, Response response) {
        this.header = header;
        this.response = response;
    }

    public ResponseHeader header() {
        return this.header;
    }

    public ResponseBody body() {
        return this.response.body();
    }

    public Response response() {
        return this.response;
    }

    public Map<String, List<String>> httpHeaders() {
        return this.response.headers().toMultimap();
    }
}
