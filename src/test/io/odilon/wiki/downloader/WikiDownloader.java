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
package io.odilon.wiki.downloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.StructuredDataMessage;

 



import io.odilon.log.Logger;
import io.odilon.test.base.BaseTest;

public class WikiDownloader extends BaseTest {
			
	static private Logger logger = Logger.getLogger(WikiDownloader.class.getName());
	
	private final CloseableHttpAsyncClient client;
	private final URI commons;
	private CountDownLatch latch;

	private String file;
	private String charset = "UTF-8";
	private String destination = "c:\temp" + File.separator + "wiki" ;
	private Mode mode = Mode.RESTART;

	
	
	public enum Mode {
		RESUME,
		RESTART
	}
	
	public Path getFile() {
		return Paths.get(file);
	}
	
	public Path getDestination() {
		return Paths.get(destination);
	}
	
	public Charset getCharset() {
		return Charset.forName(charset);
	}
	
	public Mode getMode() {
		return mode;
	}
	
	
	// ------------------
	
	@Override
	public void executeTest() {

		WikiDownloader downloader;
		
		try {

			downloader = new WikiDownloader();
			downloader.download();
			
		} catch (URISyntaxException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	
	public WikiDownloader() throws URISyntaxException {
		this.client = HttpAsyncClients.createDefault();
		this.commons = new URIBuilder("https://commons.wikimedia.org/w/index.php").addParameter("title", "Special:FilePath").build();
	}

	
	public void download() throws IOException {
	
		this.client.start();
		
		try {
												
			final int lines = (int) Files.lines(getFile(), getCharset()).count();
			
			logger.info("Will download files -> " + String.valueOf(lines));
			
			this.latch = new CountDownLatch(lines);
			Files.lines(getFile(), getCharset())
				.parallel()
				.map(s -> s.substring(0, s.lastIndexOf(',')))
				.forEach(s -> this.download(s, getDestination()));
			try {
				logger.info("Waiting for downloads to complete");
				latch.await();
			} catch (final InterruptedException e) {
				Thread.interrupted();
			}
		} catch (final Throwable t) {
			logger.error(t);
		} finally {
			logger.info("All done");
			this.client.close();
		}
	}
	
	private void download(final String fileName, final Path destination) {
		final StructuredDataMessage event = new StructuredDataMessage(Integer.toHexString(fileName.hashCode()) , null, "download");
		final Path destinationFile = Paths.get(destination.toString(), fileName.replaceAll("[:*?\"<>|/\\\\]", "_"));
		event.put("destinationFile", destinationFile.toAbsolutePath().toString());
		if (getMode() == Mode.RESTART || !destinationFile.toFile().exists()) {
			try {
				final URI uri = new URIBuilder(commons).addParameter("file", fileName).build();
				event.put("uri", uri.toString());
				final HttpGet request = new HttpGet(uri);
				client.execute(request, new DownloaderCallback(fileName, destination, event));
			} catch (final URISyntaxException e) {
				event.put("status", "error");
				EventLogger.logEvent(event, Level.WARN);
				logger.warn(e);
				latch.countDown();
			}
		} else {
			event.put("status", "skipped");
			EventLogger.logEvent(event, Level.DEBUG);
			latch.countDown();
		}
	}
	
	

	private final class DownloaderCallback implements FutureCallback<HttpResponse> {
		
		private final String fileName;
		private final Path destination;
		private final StructuredDataMessage event;
		
		public DownloaderCallback(final String fileName, final Path destination, final StructuredDataMessage event) {
			this.fileName = fileName;
			this.destination = destination;
			this.event = event;
		}
		
		@Override
		public void failed(final Exception e) {
			latch.countDown();
			event.put("status", "error");
			EventLogger.logEvent(event, Level.WARN);
			logger.warn(e);
		}

		@Override
		public void completed(final HttpResponse response) {
			final Path destinationFile = getDestinationFile();
			try (final BufferedOutputStream os = new BufferedOutputStream(
					Files.newOutputStream(destinationFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
				) {
				response.getEntity().writeTo(os);
				event.put("status", "success");
				EventLogger.logEvent(event, Level.INFO);
			} catch (final IOException e) {
				event.put("status", "error");
				EventLogger.logEvent(event, Level.WARN);
				logger.warn(e);
			} finally {
				latch.countDown();
			}
		}

		private Path getDestinationFile() {
			return Paths.get(destination.toString(), fileName.replaceAll("[:*?\"<>|/\\\\]", "_"));
		}

		@Override
		public void cancelled() {
			event.put("status", "cancelled");
			EventLogger.logEvent(event, Level.DEBUG);
			latch.countDown();
		}
	}



	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
