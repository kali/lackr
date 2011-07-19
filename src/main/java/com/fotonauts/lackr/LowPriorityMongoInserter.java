package com.fotonauts.lackr;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

public class LowPriorityMongoInserter {

    protected Thread thread;

    protected Queue<DBObject> queue;

    public LowPriorityMongoInserter(final DBCollection collection) {
        if(collection == null)
            throw new IllegalArgumentException("unexpected null collection object");
        this.queue = new ConcurrentLinkedQueue<DBObject>();
        this.thread = new Thread("mongo-async-inserter-" + collection.getName()) {
            public void run() {
                WriteConcern concern = new WriteConcern(-1);
                DBObject[] objects = new DBObject[4096];
                while(true) {
                    try {
                        DBObject object;
                        int i = 0;
                        while(i < objects.length && (object = queue.poll()) != null) {
                            objects[i++] = object;
                        }
                        if(i == objects.length) {
                            System.err.println("Overflow while writting to mongo. Clearing backqueue.");
                            queue.clear();
                        }
                        if(i == 0)
                            Thread.sleep(10);
                        else
                            collection.insert(Arrays.copyOfRange(objects, 0, i), concern);
                    } catch (Exception e) {
                        System.err.println("error in insertion loop for " + collection.getName());
                        e.printStackTrace(System.err);
                    }
                }
            };
        };
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void save(DBObject object) {
        queue.offer(object);
    }
}
