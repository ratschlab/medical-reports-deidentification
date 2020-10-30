package org.ratschlab.deidentifier.workflows;

import gate.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class WorkflowConcernWithQueue<T> extends DefaultWorkflowConcern {
    private int batchSize = 100;

    private Queue<T> writeQueue = new ConcurrentLinkedQueue<>();
    private AtomicBoolean isFirstFlush = new AtomicBoolean(true);

    public boolean addToQueue(T item) {
        if (writeQueue.size() >= batchSize) {
            flushQueue();
        }

        return writeQueue.add(item);
    }

    @Override
    public void doneHook() {
        flushQueue();
    }

    private synchronized void flushQueue() {
        boolean flush = isFirstFlush.getAndSet(false);

        List<T> lst = new ArrayList<>();

        while (!writeQueue.isEmpty()) {
            lst.add(writeQueue.poll());
        }

        queueFlushAction(lst, flush);
    }

    abstract protected void queueFlushAction(List<T> items, boolean firstFlush);
}
