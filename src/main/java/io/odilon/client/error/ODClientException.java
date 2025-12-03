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
package io.odilon.client.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.odilon.net.ErrorCode;

//import tools.jackson.databind.ObjectMapper;

/**
 * <p>
 * Exception thrown by the Odilon client library.<br/>
 * It contains three main parts:
 * </p>
 * <ul>
 * <li>The code of the {@link io.odilon.net.ODHttpStatus ODHttpStatus} status
 * ({@code int})</li>
 * <li>The code of the Odilon {@link io.odilon.net.ErrorCode ErrorCode}
 * ({@code int})</li>
 * <li>Odilon error message ({@code String})</li>
 * </ul>
 * 
 *
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class ODClientException extends Exception {

    private static final long serialVersionUID = 1L;

    static private ObjectMapper mapper = new ObjectMapper();

    static {
    	mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @JsonProperty("httpStatus")
    private int httpStatus;

    @JsonProperty("odilonErrorCode")
    private int odilonErrorCode;

    @JsonProperty("odilonErrorMessage")
    private String odilonErrorMessage;

    @JsonProperty("context")
    private Map<String, String> context = new HashMap<String, String>();

    public ODClientException(Exception e) {
        super(e);
        this.httpStatus = 0;
        this.odilonErrorCode = ErrorCode.CLIENT_ERROR.value();
        this.odilonErrorMessage = e.getClass().getName() + (e.getMessage() != null ? (" | " + e.getMessage()) : "");
    }

    public ODClientException(int odilonErrorCode, String odilonErrorMessage) {
        super(odilonErrorMessage);
        this.httpStatus = 0;
        this.odilonErrorCode = odilonErrorCode;
        this.odilonErrorMessage = odilonErrorMessage;
    }

    public ODClientException(int httpStatus, int odilonErrorCode, String odilonErrorMessage) {
        super(odilonErrorMessage);
        this.httpStatus = httpStatus;
        this.odilonErrorCode = odilonErrorCode;
        this.odilonErrorMessage = odilonErrorMessage;
    }

    public String getMessage() {
        return odilonErrorMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getErrorCode() {
        return odilonErrorCode;
    }

    public void setErrorCode(int odilonErrorCode) {
        this.odilonErrorCode = odilonErrorCode;
    }

    public void setContext(Map<String, String> s) {
        context = s;
    }

    public Map<String, String> getContext() {
        return context;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getSimpleName() + "{");
        str.append("\"httpStatus\":" + String.valueOf(httpStatus));
        str.append(", \"odilonErrorCode\": " + String.valueOf(odilonErrorCode));
        str.append(", \"odilonErrorMessage\": \"" + String.valueOf(Optional.ofNullable(odilonErrorMessage).orElse("null")) + "\"");
        str.append("}");
        return str.toString();
    }

    public String toJSON() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "\"error\":\"" + e.getClass().getName() + " | " + e.getMessage() + "\"";
        }
    }
}
