/*
 * Odilon Object Storage
 * (c) kbee 
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
import java.io.InputStream;


/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
@Deprecated
public class HttpFileEntity implements HttpEntity {

	private String name;
	private InputStream stream;
	private long size;
    
    public HttpFileEntity(InputStream stream, String name, long size) {
    	this.stream=stream;
    	this.name=name;
    	this.size=size;
    }
    
    public InputStream getStream() throws IOException {
        return stream;
    }

    @Override
    public long getSize() {
        return size;
    }

	@Override
	public String getName() {
		return name;
	}
}
