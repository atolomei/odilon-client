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

/**
 * <p>
 * HTTP schemes
 * </p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public enum Scheme {
    HTTP("http"), HTTPS("https");

    private final String value;

    private Scheme(String value) {
        this.value = value;
    }

    /**
     * <p>
     * Returns Scheme enum of given string
     * </p>
     */
    public static Scheme fromString(String scheme) {
        if (scheme == null) {
            throw new IllegalArgumentException("null scheme");
        }

        for (Scheme s : Scheme.values()) {
            if (scheme.equalsIgnoreCase(s.value)) {
                return s;
            }
        }

        throw new IllegalArgumentException("invalid HTTP scheme '" + scheme + "'");
    }
}