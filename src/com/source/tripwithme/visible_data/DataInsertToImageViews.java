package com.source.tripwithme.visible_data;

import android.widget.ImageView;
import com.source.tripwithme.components.ResourceFlagedItem;

public class DataInsertToImageViews {

    public static int NO_RESOUCE_IDENTIFIER = 0;


    public static void fillImagesViewsWithData(ImageView[] imageViews, ResourceFlagedItem[] data,
                                               int resorceWhenNoData) {
        if (imageViews == null || data == null) {
            return;
        }
        int dataCounter = 0;
        for (ImageView imgIcon : imageViews) {
            if (dataCounter < data.length) {
                imgIcon.setImageResource(data[dataCounter++].resource());
                imgIcon.setVisibility(ImageView.VISIBLE);
            } else if (resorceWhenNoData != NO_RESOUCE_IDENTIFIER) {
                imgIcon.setImageResource(resorceWhenNoData);
                imgIcon.setVisibility(ImageView.VISIBLE);
            } else {
                imgIcon.setVisibility(ImageView.INVISIBLE);
            }
        }

    }

}
