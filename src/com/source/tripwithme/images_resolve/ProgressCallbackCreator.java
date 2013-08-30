package com.source.tripwithme.images_resolve;

import com.parse.ProgressCallback;

import java.util.concurrent.atomic.AtomicInteger;


public class ProgressCallbackCreator {


    private final ProgressCallback listenerCallback;
    private final float magnitude;
    private final int numberOfFiles;
    private AtomicInteger progressAtomic;
    private AtomicInteger filesDone;

    public ProgressCallbackCreator(ProgressCallback listenerCallback, int numberOfFiles) {
        this.listenerCallback = listenerCallback;
        this.numberOfFiles = numberOfFiles;
        magnitude = 1.0f / numberOfFiles;
        progressAtomic = new AtomicInteger();
        filesDone = new AtomicInteger(0);


    }

    public ProgressCallback spawnBasedOnKnowNumber() {

        return new ProgressCallback() {
            private int lastValue = 0;
            private float summed = 0;
            private boolean done = false;

            @Override
            public void done(Integer integer) {
                int toAdd = integer - lastValue;
                lastValue = integer;
                summed += toAdd * magnitude;   // don't lose all numbers by the floor - cluster them
                if (summed >= 1) {
                    listenerCallback.done(progressAtomic.addAndGet((int)Math.floor(summed)));
                    summed = 0;
                }
                if (integer == 100 && !done) {  // can they send twice 100 ?
                    done = true;
                    filesDone.incrementAndGet();
                }
            }
        };

    }


    public boolean isDone() {
        return filesDone.get() == numberOfFiles;
    }


}
