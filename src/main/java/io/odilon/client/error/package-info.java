/*
 * Odilon Java SDK 
 * (C) 2023 Novamens 
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

/**
 * <p>Error management for client server interaction.</p>
 * 
 * <p>{@link ODClientException} has the following fields:</p>
 * 
 * <ul>
 * <li>The code of the HTTP status of the request to the server {@link io.odilon.net.ODHttpStatus ODHttpStatus} status ({@code int})</li>
 * <li>The code of the specific Odilon error {@link  io.odilon.net.ErrorCode ErrorCode} ({@code int})</li>
 * <li>A string with the error message returned by the server ({@code String})</li>
 * </ul>
 * 
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
package io.odilon.client.error;

