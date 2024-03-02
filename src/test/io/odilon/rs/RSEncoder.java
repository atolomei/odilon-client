package io.odilon.rs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import io.odilon.client.util.FSUtil;
import io.odilon.log.Logger;
import io.odilon.util.Check;

public class RSEncoder {

	static private Logger logger = Logger.getLogger(RSEncoder.class.getName());

	private String SRC_DIR = "d:"+File.separator+"test-files"+File.separator+"v0";
	private String DECODE_DIR = "d:"+File.separator+"test-files-rs-decode";
	private String 	ENCODE_DIR = "d:"+File.separator+"test-files-rs-encode";

    int MAX = 5;
	long MAX_LENGTH =20 * 100 * 10000; // 20 MB
	
	public static final double KB = 1024.0;
	public static final double MB = 1024.0 * KB;
	public static final double GB = 1024.0 * MB;

	public static final int iKB = 1024;
	public static final int iMB = 1024 * iKB;
	public static final int iGB = 1024 * iMB;


    public static final int BYTES_IN_INT = 4;
    public static final int BYTES_IN_LONG = 16;
    
    public static final int MAX_CHUNK_SIZE = 48 * iMB;

    public static final int DATA_SHARDS = 4;
    public static final int PARITY_SHARDS = 2;
    public static final int TOTAL_SHARDS = 6;

    
    long fileSize = 0;
    int chunk = 0;
    List<String> objectName = new ArrayList<String>();
    List<String> fileName = new ArrayList<String>();
    

    /**
     * 
     */
    public static void main(String [] arguments) {
    	 
    	RSEncoder enc = new RSEncoder();
    
    	enc.preCondition();
    	enc.test();
    	 
    }
    
	
    public void test() {

        File dir = new File(SRC_DIR);
        
        if ((!dir.exists()) || (!dir.isDirectory())) { 
			throw new RuntimeException("Dir not exists or the File is not Dir -> " +SRC_DIR);
		}
    	
        int counter = 0;
        
        logger.debug("Encoding");
        
		// put files
		//
		for (File fi:dir.listFiles()) {
			

			if (counter >= MAX)
				break;
			
			if (isElegible(fi)) {
				
				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fi))) {
					String oname = FSUtil.getBaseName(fi.getName());
					
					encode(inputStream,  oname);
					
					objectName.add(oname);
					fileName.add(fi.getName());
					
					logger.debug("enc -> " + fi.getName());
					
				} catch (FileNotFoundException e) {
					logger.error(e);
				} catch (IOException e) {
					logger.error(e);
				}
				counter++;
			}
		}
		
		
		logger.debug("Decoding");
		
		for (int n=0; n<objectName.size();n++) {
			decode(objectName.get(n), fileName.get(n));
			logger.debug("dec -> " + fileName.get(n));
			
		}
        
    }
    
    
    
    
    public void RSEnconder() {
    }
    
    
    /**
     * @param is
     */
    public boolean encodeChunk(InputStream is, String objectName, int chunk) {

    	final byte [] allBytes = new byte[ MAX_CHUNK_SIZE ];

    	int bytesRead = 0;
        
		try {
		
			bytesRead = is.read(allBytes, BYTES_IN_INT, MAX_CHUNK_SIZE - BYTES_IN_INT);
			
		} catch (IOException e) {
				logger.error(e);
				System.exit(1);
		}

		if (bytesRead==0)
			return false;
			
		this.fileSize += bytesRead;

		ByteBuffer.wrap(allBytes).putInt(bytesRead);
		
		final int storedSize = bytesRead + BYTES_IN_INT;
		final int shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS;
		
		byte [] [] shards = new byte [TOTAL_SHARDS] [shardSize];
		
        // Fill in the data shards
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }
                
        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);

        
        // Write out the resulting files.
        for (int disk = 0; disk < TOTAL_SHARDS; disk++) {

        	File outputFile = new File(ENCODE_DIR, objectName + "." + String.valueOf(chunk)+"." + String.valueOf(disk));
        							
			try  (OutputStream out = new FileOutputStream(outputFile)) {

				out.write(shards[disk]);
				
	        } catch (FileNotFoundException e) {
				logger.error(e);
				System.exit(1);
			} catch (IOException e) {
				logger.error(e);
				System.exit(1);
			}
        }
        
		if (bytesRead<(MAX_CHUNK_SIZE - BYTES_IN_INT))
			return true;

        return false;
        
    	
    }

    public boolean decodeChunk(String objectName, int chunk, OutputStream out) {
    	
    	 
    	// Read in any of the shards that are present.
        // (There should be checking here to make sure the input
        // shards are the same size, but there isn't.)
        final byte [] [] shards = new byte [TOTAL_SHARDS] [];
        final boolean [] shardPresent = new boolean [TOTAL_SHARDS];
        int shardSize = 0;
        int shardCount = 0;
        
        for (int disk = 0; disk < TOTAL_SHARDS; disk++) {
        	File shardFile = new File(
        			ENCODE_DIR,
                    objectName + "." + String.valueOf(chunk)+"." + String.valueOf(disk));
           
            if (shardFile.exists()) {
                shardSize = (int) shardFile.length();
                shards[disk] = new byte [shardSize];
                shardPresent[disk] = true;
                shardCount += 1;
    			try (InputStream in = new FileInputStream(shardFile)) {
					in.read(shards[disk], 0, shardSize);
				} catch (FileNotFoundException e) {
					logger.error(e);
					System.exit(1);
				} catch (IOException e) {
					logger.error(e);
					System.exit(1);
				}
            }
        }
        
        
        // We need at least DATA_SHARDS to be able to reconstruct the file.
        if (shardCount < DATA_SHARDS) {
            throw new RuntimeException("We need at least DATA_SHARDS to be able to reconstruct the file.");
        }
        
        // Make empty buffers for the missing shards.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            if (!shardPresent[i]) {
                shards[i] = new byte [shardSize];
            }
        }
        
        // Use Reed-Solomon to fill in the missing shards
        ReedSolomon reedSolomon = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        
        // Combine the data shards into one buffer for convenience.
        // (This is not efficient, but it is convenient.)
        byte [] allBytes = new byte [shardSize * DATA_SHARDS];
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
        }

        // Extract the file length
        int fileSize = ByteBuffer.wrap(allBytes).getInt();

        try {
			out.write(allBytes, BYTES_IN_INT, fileSize);
		} catch (IOException e) {
            throw new RuntimeException("Not enough shards present");
		}

		if (shardSize<(MAX_CHUNK_SIZE - BYTES_IN_INT))
			return true;
		
    	return false;
    	
    }

    
    /**
     * @param is
     */
    public void decode(String oname, String fileName) {
    	
    	Check.requireNonNull(oname);
    	
    	int chunk = 0;
    	
    	boolean done = false;
    	
    	try (OutputStream out = new BufferedOutputStream(new FileOutputStream(DECODE_DIR + File.separator + fileName))) {
    	
    		while (!done) {
        		done = decodeChunk(oname, chunk++, out);
        	}
    		
    	} catch (FileNotFoundException e) {
    		logger.error(e);
			System.exit(1);
		} catch (IOException e) {
			logger.error(e);
			System.exit(1);
		}
    }

    	
    	
    /**
     * 
     * @param is
     */
    public void encode(InputStream is, String objectName) {
    
    	Check.requireNonNull(is);
    	Check.requireNonNull(objectName);
    	
    	this.fileSize = 0;
    	this.chunk = 0;
        
    	boolean done = false;
    	
    	try (is) {
	    	while (!done) { 
	    		done = encodeChunk(is, objectName, chunk++);
	    	}
	    } catch (Exception e) {
	    		logger.error(e);
	    }
    }

    /**
     * 
     * 
     */
    private void preCondition() {

    	{
	    	File tmpdir = new File(SRC_DIR);
	        
	    	
	    	if ( (!tmpdir.exists()) || (!tmpdir.isDirectory())) {
	    		try {
					FileUtils.forceMkdir(tmpdir);
				} catch (IOException e) {
					logger.error(e.getClass().getName() + " | " + e.getMessage());
					System.exit(1);
				}	
	    	}
    	}
    	
    	
    	{								
	    	File tmpdir = new File(ENCODE_DIR);
	
	    	if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
		        	try {
						FileUtils.forceDelete(tmpdir);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " | " + e.getMessage());
						System.exit(1);
					}
				}
		    	
	    	try {
					FileUtils.forceMkdir(tmpdir);
			} catch (IOException e) {
					logger.error(e.getClass().getName() + " | " + e.getMessage());
					System.exit(1);
			}	
    	}
    	

    	{
	    	File tmpdir = new File(DECODE_DIR);
	
	    	if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
		        	try {
						FileUtils.forceDelete(tmpdir);
					} catch (IOException e) {
						logger.error(e.getClass().getName() + " | " + e.getMessage());
						System.exit(1);
					}
				}
		    	
	    	try {
					FileUtils.forceMkdir(tmpdir);
			} catch (IOException e) {
					logger.error(e.getClass().getName() + " | " + e.getMessage());
					System.exit(1);
			}	
    	}
    	
    }

	private boolean isElegible(File file) {
		
		if (file.isDirectory())
			return false;
		
		if (file.length()>MAX_LENGTH)
			return false;
		
		if (	FSUtil.isText(file.getName()) || 
				FSUtil.isText(file.getName()) || 
				FSUtil.isPdf(file.getName())  || 
				FSUtil.isImage(file.getName()) || 
				FSUtil.isMSOffice(file.getName()) || 
				FSUtil.isZip(file.getName()))
			
			return true;
		
		return false;
	}

	

	
}
