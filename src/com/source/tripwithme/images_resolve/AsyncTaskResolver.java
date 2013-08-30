package com.source.tripwithme.images_resolve;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import com.parse.ProgressCallback;
import com.source.tripwithme.visible_data.BaseVisibleData;

public class AsyncTaskResolver extends AsyncTask<Void, Integer, Void> {


    private static final int SLEEP_TO_VALIDATE_END = 100;
    private static final long SLOWNESS_TIME_MILIS = 20 * 1000;
    private static final Integer ESCAPE_OPTION = -1;
    private final ProgressDialog dialog;
    private final PostExecuter postExecuter;
    private final BaseVisibleData toResolve;
    private final Context context;
    private AlertDialog alertDialogSlowness;
    private boolean userRequestedDone;

    public AsyncTaskResolver(Context context, PostExecuter postExecuter, BaseVisibleData toResolve) {
        dialog = new ProgressDialog(context);
        this.postExecuter = postExecuter;
        this.toResolve = toResolve;
        this.context = context;
        userRequestedDone = false;
        alertDialogSlowness = null;
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Please Wait, downloading photos...");
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        // TODO to resolve can be null after quiting app and returning?
        if (toResolve == null) {
            return null; //?!
        }
        ProgressCallbackCreator creator = toResolve.preResolveAll(new ProgressCallback() {
            public void done(Integer percentDone) {
                publishProgress(percentDone);
            }
        });
        try {
            // wait for progress to end TODO RISKY
            long startTime = System.currentTimeMillis();
            while (!creator.isDone() && !userRequestedDone) {
                long current = System.currentTimeMillis();
                if (current - startTime >= SLOWNESS_TIME_MILIS) {
                    publishProgress(ESCAPE_OPTION);
                    startTime = current;
                }
                Thread.sleep(SLEEP_TO_VALIDATE_END);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        // we have finished so alert is not needed anymore
        if (alertDialogSlowness != null && alertDialogSlowness.isShowing()) {
            alertDialogSlowness.dismiss();
        }
        dialog.dismiss();
        postExecuter.postExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int val = values[0];
        if (val == ESCAPE_OPTION) {
            if (alertDialogSlowness != null && alertDialogSlowness.isShowing()) {
                return; // already shown
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Too Long Download ( > " + SLOWNESS_TIME_MILIS / 1000 + " Seconds)")
                .setMessage("Your connection is slow, Photos later?")
                .setPositiveButton("Later", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userRequestedDone = true;
                        alertDialogSlowness.dismiss();
                    }
                })
                .setNegativeButton("Wait!", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialogSlowness.dismiss();
                    }
                });
            alertDialogSlowness = builder.create();
            alertDialogSlowness.show();

        } else {
            dialog.setProgress(val);
        }
    }

    public interface PostExecuter {

        void postExecute();
    }
}
