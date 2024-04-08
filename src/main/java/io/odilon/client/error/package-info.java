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

