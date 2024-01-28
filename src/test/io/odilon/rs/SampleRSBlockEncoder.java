package io.odilon.rs;

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
import io.odilon.test.base.BaseTest;

public class SampleRSBlockEncoder extends BaseTest {
			
	private static final Logger logger = Logger.getLogger(SampleRSBlockEncoder.class.getName());
	
	public static final int DATA_SHARDS = 4;
    public static final int PARITY_SHARDS = 2;
    public static final int TOTAL_SHARDS = 6;

    public static final int BYTES_IN_INT = 4;
    
	static final String SOURCE_DIR = "c:"+File.separator+"temp" + File.separator + "rs";
	static final String ENCODE_DIR 	= SOURCE_DIR  + File.separator+"encode";
	static final String DECODE_DIR 	= SOURCE_DIR + File.separator + "rs" + File.separator+"decode";
	
	static final int BUFFER_SIZE = 4096;
	static final long MAX_LENGTH = 100 * 10000; // 1 MB

	private final File encodeDir = new File(ENCODE_DIR);

	private List<File> testFiles = new ArrayList<File>();
	
	@Override
	public void executeTest() {
		preCondition();
		encode();
		decode();
	}
	
	
	public void encode() {
		
		final File dir = new File(SOURCE_DIR);
		
		if ((!dir.exists()) || (!dir.isDirectory()))  
			error("Dir not exists or the File is not Dir -> " +SOURCE_DIR);
		
		if ( (!encodeDir.exists()) || (!encodeDir.isDirectory())) {
	        try {
				FileUtils.forceMkdir(encodeDir);
			} catch (IOException e) {
					logger.error(e);
					error(e.getClass().getName() + " | " + e.getMessage());
			}
        }

		
		File inputFile = null;
		
		for (File fi:dir.listFiles()) {
			if (!fi.isDirectory() && (FSUtil.isPdf(fi.getName()) || FSUtil.isImage(fi.getName()) || FSUtil.isZip(fi.getName())) && (fi.length()<MAX_LENGTH)) {
				inputFile=fi;
				break;
			}
		}

		if (inputFile==null) {
			error("No file to encode");
		}
		if (!inputFile.exists()) {
            error("Cannot read input file: " + inputFile);
        }
		
		// Get the size of the input file.  (Files bigger that Integer.MAX_VALUE will fail here!)
        final int fileSize = (int) inputFile.length();

        // Figure out how big each shard will be. The total size stored
        // will be the file size (8 bytes) plus the file.
        final int storedSize = fileSize + BYTES_IN_INT;
        final int shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS;

        // Create a buffer holding the file size, followed by
        // the contents of the file.
        final int bufferSize = shardSize * DATA_SHARDS;
        final byte [] allBytes = new byte[ bufferSize ];
	
        ByteBuffer.wrap(allBytes).putInt(fileSize);
        
        // ---------------------
        
        InputStream in = null;
        int bytesRead;
		try {
			in = new FileInputStream(inputFile);
			bytesRead = in.read(allBytes, BYTES_IN_INT, fileSize);
			if (bytesRead != fileSize) {
	            throw new IOException("not enough bytes read");
	        }
			testFiles.add(inputFile);
		} catch (IOException e) {
			error(e);
		}
		finally {
			if (in!=null) {
				try {
					in.close();
				} catch (IOException e) {
					error(e);
				}
			}
		}
        
		boolean done = false;
		
		while (!done) {
			// read buffer
			
			
			
			
			
		}
		
		/**
		  while (!done) {
			  if ( buffer < BUFFER_SIZE ) {
                 // llena de 0000 y size (4 bytes)
                 // si no entran 4 bytes, 
                 // llena de ceros y agrega un block nuevo con 000 y size (4 bytes) 
			  }
			  lee buffer
			  encodea buffer en 6 buffers
			  graba 6 buffers
		  }
		 */
		
        // ---------------------

        // Make the buffers to hold the shards.
        byte [] [] shards = new byte [TOTAL_SHARDS] [shardSize];

        // Fill in the data shards
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);

        // Write out the resulting files.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            File outputFile = new File(
                    inputFile.getParentFile(),
                    inputFile.getName() + "." + i);
            
            OutputStream out = null;
            
			try {
				out = new FileOutputStream(outputFile);
			} catch (FileNotFoundException e) {
				error(e);
			}

			try {
				out.write(shards[i]);
			} catch (Exception e) {
				
			}
			finally {
				if (out!=null) {
					try {
						out.close();
					} catch (IOException e) {
						error(e);
					}
				}
			}

			
            System.out.println("wrote " + outputFile);
        }
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}
	
	/**
	 * 
	 */
	@Override
	public boolean preCondition() {

        File dir = new File(SOURCE_DIR);
        
        if ( (!dir.exists()) || (!dir.isDirectory())) { 
			error("Dir not exists or the File is not Dir -> " +SOURCE_DIR);
		}
        
        File tmpdir = new File(ENCODE_DIR);
        
        if ( (tmpdir.exists()) && (tmpdir.isDirectory())) { 
        	try {
				FileUtils.forceDelete(tmpdir);
			} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
			}
		}
       	try {
				FileUtils.forceMkdir(tmpdir);
				
		} catch (IOException e) {
				error(e.getClass().getName() + " | " + e.getMessage());
		}
		
       	return true;
        
	}
	
	public void decode() {
		
		for( File file: testFiles) {
		
			File originalFile = file;
			
	        // Read in any of the shards that are present.
	        // (There should be checking here to make sure the input
	        // shards are the same size, but there isn't.)
	        final byte [] [] shards = new byte [TOTAL_SHARDS] [];
	        final boolean [] shardPresent = new boolean [TOTAL_SHARDS];

	        int shardSize = 0;
	        int shardCount = 0;
	        
	        for (int i = 0; i < TOTAL_SHARDS; i++) {
	            
	        	File shardFile = new File(
	                    originalFile.getParentFile(),
	                    originalFile.getName() + "." + i);
	        	
	        	if (shardFile.exists()) {
	                shardSize = (int) shardFile.length();
	                shards[i] = new byte [shardSize];
	                shardPresent[i] = true;
	                shardCount += 1;
	                
	                InputStream in = null;
	                try {
		                in = new FileInputStream(shardFile);
		                in.read(shards[i], 0, shardSize);
	                }
	                catch (Exception e) {
	                	logger.error(e);
	                }
	                finally {
	                	if (in!=null) {
	        				try {
	        					in.close();
	        				} catch (IOException e) {
	        					error(e);
	        				}
	        			}
	                }

	            }
	        }
	        
            // We need at least DATA_SHARDS to be able to reconstruct the file.
            if (shardCount < DATA_SHARDS) {
                error("Not enough shards present");
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

            
            // Write the decoded file
            File decodedFile = new File(originalFile.getParentFile(), originalFile.getName() + ".decoded");
            
            OutputStream out = null;
            
            try {
			
            	out = new FileOutputStream(decodedFile);
            	out.write(allBytes, BYTES_IN_INT, fileSize);
            	
         
			} catch (Exception e) {
				error(e);
			}
	         finally {
	    				if (out!=null) {
	    					try {
	    						out.close();
	    					} catch (IOException e) {
	    						error(e);
	    					}
	    				}
	    			}
			}
		
	}	
	
	
	

}
