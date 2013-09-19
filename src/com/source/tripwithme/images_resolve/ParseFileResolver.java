package com.source.tripwithme.images_resolve;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ProgressCallback;
import com.source.tripwithme.databases.ParseUtil;

public class ParseFileResolver implements ImageResolver {

    private final ParseFile file;
    private final ParseUtil parseUtil;
    private Bitmap bitmap;
    private boolean tried;

    public ParseFileResolver(ParseFile file, ParseUtil parseUtil) {
        this.file = file;
        this.parseUtil = parseUtil;
        bitmap = null;
        tried = false;
    }


    // if problem accour resolve won't do done with 100 and another protection from above is needed
    // TODO still there is a problem when menu try to resolve and a click on person come in parallal
    @Override
    public void resolve(final ProgressCallback progressCallback, final BitmapGotCallback bitmapGotCallback) {
        if (!tried) {
            tried = true;
            if (file != null) {
                file.getDataInBackground(new GetDataCallback() {

                                             @Override
                                             public void done(byte[] bytes, ParseException e) {
                                                 if (e == null) {
                                                     try {
                                                         bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                     } catch (Exception e1) {
                                                         Log.e("Resolve", "Failure in resolving", e1);
                                                     }
                                                 } else {
                                                     Log.e("Resolve", "Failure in resolving", e);
                                                     parseUtil.showErrorInAccountDialog(e, false);
                                                 }
                                                 picIsDone(bitmapGotCallback, progressCallback);
                                             }
                                         }, new ProgressCallback() {
                                             @Override
                                             public void done(Integer integer) {
                                                 if (integer != 100) { // 100 will be given after decode is done
                                                     progressCallback.done(integer);
                                                 }
                                             }
                                         }
                );
            } else {
                picIsDone(bitmapGotCallback, progressCallback);
            }
        } else {
            picIsDone(bitmapGotCallback, progressCallback);
        }
    }

    private void picIsDone(BitmapGotCallback bitmapGotCallback, ProgressCallback progressCallback) {
        bitmapGotCallback.got(bitmap);
        progressCallback.done(100);
    }

}

