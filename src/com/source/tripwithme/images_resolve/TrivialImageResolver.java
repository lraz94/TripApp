package com.source.tripwithme.images_resolve;

import android.graphics.Bitmap;
import com.parse.ProgressCallback;


public class TrivialImageResolver implements ImageResolver {

    private final Bitmap bitmap;

    public TrivialImageResolver(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public void resolve(ProgressCallback progressCallback, BitmapGotCallback bitmapGotCallback) {
        progressCallback.done(100);
        bitmapGotCallback.got(bitmap);
    }
}
