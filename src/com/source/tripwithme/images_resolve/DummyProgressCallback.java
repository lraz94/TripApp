package com.source.tripwithme.images_resolve;

import com.parse.ProgressCallback;


public class DummyProgressCallback extends ProgressCallback {

    @Override
    public void done(Integer integer) {
        //  nothing

    }

    public static ProgressCallbackCreator creator(int length) {
        ProgressCallback dummyProgress = new DummyProgressCallback();
        return new ProgressCallbackCreator(dummyProgress, length);
    }
}
