package io.odilon.client.upload;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamRequestBody extends RequestBody {

	private static final int DEFAULT_CHUNK_LEN = 16 * 1024;
	
	private final MediaType contentType;
    private final InputStream inputStream;

    public InputStreamRequestBody(MediaType contentType, InputStream inputStream) {
        this.contentType = contentType;
        this.inputStream = inputStream;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return -1; // Unknown length
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try {
            byte[] buffer = new byte[DEFAULT_CHUNK_LEN];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
            }
        } finally {
            inputStream.close();
        }
    }
    
}
