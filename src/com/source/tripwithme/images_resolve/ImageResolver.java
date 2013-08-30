package com.source.tripwithme.images_resolve;

import com.parse.ProgressCallback;

public interface ImageResolver {

    // return true if have data from net
    public void resolve(ProgressCallback allForThisCallback, BitmapGotCallback bitmapGotCallback);

}
