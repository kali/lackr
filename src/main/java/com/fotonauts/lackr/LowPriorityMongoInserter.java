package com.fotonauts.lackr;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

public class LowPriorityMongoInserter {

    protected Thread thread;

    protected Queue<DBObject> queue;

    public LowPriorityMongoInserter(final DBCollection collection) {
        this.queue = new ConcurrentLinkedQueue<DBObject>();
        this.thread = new Thread() {
            public void run() {
                WriteConcern concern = new WriteConcern(-1);
                DBObject[] objects = new DBObject[256];
                while(true) {
                    DBObject object;
                    int i = 0;
                    while(i < objects.length && (object = queue.poll()) != null) {
                        objects[i++] = object;
                    }
                    if(i == objects.length) {
                        System.out.println("Overflow while writting to mongo. Clearing backqueue.");
                        queue.clear();
                    }
                    if(i == 0)
                        Thread.yield();
                    else
                        collection.insert(objects, concern);
                }
            };
        };
        this.thread.setDaemon(true);
    }

    public void save(DBObject object) {
        queue.offer(object);
    }
}
