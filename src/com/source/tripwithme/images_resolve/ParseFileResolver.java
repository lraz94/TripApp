package com.source.tripwithme.images_resolve;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

    private static final long RESOLVE_SLEEP_MILLIS = 100;
    private boolean got;
    private boolean exceptionsThrown;

    public ParseFileResolver(ParseFile file, ParseUtil parseUtil) {
        this.file = file;
        this.parseUtil = parseUtil;
        bitmap = null;
        tried = false;
        got = false;
        exceptionsThrown = false;
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
                                                     bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                     bitmapGotCallback.got(bitmap);
                                                     got = true;
                                                 } else {
                                                     exceptionsThrown = true;
                                                     e.printStackTrace();
                                                     parseUtil.showErrorInAccountDialog(e, false);
                                                 }
                                             }
                                         }, new ProgressCallback() {
                                             @Override
                                             public void done(Integer integer) {
                                                 if (integer == 100) {
                                                     sleepTillGotInBackgroud(progressCallback);
                                                 } else {
                                                     progressCallback.done(integer);
                                                 }
                                             }
                                         }
                );
            } else {
                progressCallback.done(100);
                bitmapGotCallback.got(bitmap);
            }
        } else {
            progressCallback.done(100);
            bitmapGotCallback.got(bitmap);
        }
    }

    private void sleepTillGotInBackgroud(final ProgressCallback progressCallback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // if exception thrown or we got - done!
                    while (!exceptionsThrown && !got) {
                        Thread.sleep(RESOLVE_SLEEP_MILLIS);
                    }
                } catch (InterruptedException ignored) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                progressCallback.done(100);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

