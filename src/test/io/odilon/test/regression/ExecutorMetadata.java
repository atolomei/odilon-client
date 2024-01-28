package io.odilon.test.regression;

import io.odilon.client.ODClient;
import io.odilon.client.error.ODClientException;
import io.odilon.log.Logger;
import io.odilon.model.Bucket;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.list.Item;
import io.odilon.model.list.ResultSet;

public class ExecutorMetadata extends Executor {
			
	static public Logger logger = Logger.getLogger(ExecutorMetadata.class.getName());
	
	private Bucket bucket;
	
	public ExecutorMetadata(long sleepTime, ODClient client, Bucket bucket) {
		super(sleepTime, client, bucket);
	}

	public void init() {
		try {
			this.bucket=getClient().getBucket(getTestBucket().getName());
		} catch (ODClientException e) {
			 error(e);
		}
	}

	@Override
	public void executeTask() {
	
		try {
			
			ResultSet<Item<ObjectMetadata>> rs=getClient().listObjects(bucket);
			Item<ObjectMetadata> item;
			
			int counter = 0;
			
			while (rs.hasNext() && counter++<5) {
				item = rs.next();
				if (item.isOk())
					logger.debug( item.getObject().toString());
				else 
					logger.error(item.getErrorString());
			}
			logger.debug( this.getClass().getSimpleName() + " -> listObjects ok");
			
		} catch (ODClientException e) {
			 error(e);
		}
	}

}
