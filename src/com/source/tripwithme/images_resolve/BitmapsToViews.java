package com.source.tripwithme.images_resolve;


import android.graphics.Bitmap;
import android.widget.ImageView;
import com.source.tripwithme.visible_data.DataInsertToImageViews;

public class BitmapsToViews extends BitmapGotCallback {

    private final ImageView imageView;
    private final int resource;

    public BitmapsToViews(ImageView imageView) {
        this(imageView, DataInsertToImageViews.NO_RESOUCE_IDENTIFIER);
    }

    public BitmapsToViews(ImageView imageView, int resource) {
        this.resource = resource;
        this.imageView = imageView;

    }

    public void got(Bitmap bitmap) {
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else if (resource != DataInsertToImageViews.NO_RESOUCE_IDENTIFIER) {
            imageView.setImageResource(resource);
        }
    }

    public static BitmapGotCallback[] createArray(ImageView[] imageViews, int length) {
        BitmapGotCallback[] bitmapGotCallbacks = new BitmapGotCallback[length];
        for (int i = 0; i < length; i++) {
            bitmapGotCallbacks[i] = new BitmapsToViews(imageViews[i]);
        }
        return bitmapGotCallbacks;
    }
}

