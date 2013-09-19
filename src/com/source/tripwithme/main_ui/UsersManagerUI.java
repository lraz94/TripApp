package com.source.tripwithme.main_ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.layout;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.databases.ParseUtil;
import com.source.tripwithme.images_resolve.AsyncTaskResolver;
import com.source.tripwithme.images_resolve.AsyncTaskResolver.PostExecuter;
import com.source.tripwithme.images_resolve.BitmapsToViews;
import com.source.tripwithme.images_resolve.DummyProgressCallback;
import com.source.tripwithme.visible_data.PersonVisibleData;

public class UsersManagerUI {

    private static final int FORGOT_PASS_STATE = 1;
    private static final int REGULAR_LOGIN = 2;
    private static final int PRIMARY_PHOTO_INDEX = -1;
    private static final float MAX_HEIGHT_NEEDED_FOR_PIC = 100;
    private static final float MAX_WIDTH_NEEDED_FOR_PIC = 100;
    private static ImageView lastRequstedImageView;
    private static boolean[] selectedImages;
    private static int lastImageIndex;
    private static boolean primarySelected;
    private static Handler photoRequestHandler;

    public static void showUpdateSignupDialog(final Context context, final ParseUtil parseUtil,
                                              final Handler photoRequestHandler, final PersonVisibleData me,
                                              final boolean asMust) {
        UsersManagerUI.photoRequestHandler = photoRequestHandler;
        new AsyncTaskResolver(context, new PostExecuter() {
            @Override
            public void postExecute() {
                showDialog(context, parseUtil, me, asMust);
            }
        }, me).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void showDialog(Context context, ParseUtil parseUtil, PersonVisibleData me, boolean asMust) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layout.update);
        EditText usernameEdit = (EditText)dialog.findViewById(id.usernameedit);
        EditText passwordEdit = (EditText)dialog.findViewById(id.passwordedit);
        EditText emailEdit = (EditText)dialog.findViewById(id.emiledit);
        ImageView[] imageViews = new ImageView[]{(ImageView)dialog.findViewById(id.userimg1),
            (ImageView)dialog.findViewById(id.userimg2), (ImageView)dialog.findViewById(id.userimg3)};
        ImageView primaryImage = (ImageView)dialog.findViewById(id.primaryImageUpdate);
        addUserInfoOnViews(me, usernameEdit, passwordEdit, emailEdit, imageViews, primaryImage, parseUtil);
        setUpdateButton(context, parseUtil, me, dialog, usernameEdit, passwordEdit, emailEdit,
                        imageViews, primaryImage);
        setLoginButton(context, parseUtil, me, dialog);
        setLoginWithFacebookButton(context, parseUtil, me, dialog);
        setImageViews(imageViews, primaryImage);
        if (!asMust) {
            dialog.setTitle("Your Account");
        } else {
            dialog.setTitle("You must Login");
        }
        dialog.show();
    }

    private static void setLoginWithFacebookButton(final Context context, final ParseUtil parseUtil,
                                                   final PersonVisibleData me, final Dialog dialog) {
        Button loginFacebookButton = (Button)dialog.findViewById(id.facebookloginbutton);
        loginFacebookButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                parseUtil.loginWithFacebook(me, new ActionDoneCallback() {
                    @Override
                    public void done() {
                        Toast.makeText(context, "You are now logged in with Facebook!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private static void addUserInfoOnViews(PersonVisibleData me, EditText usernameEdit, EditText passwordEdit,
                                           EditText emailEdit, final ImageView[] imageViews, ImageView primaryImage,
                                           ParseUtil parseUtil) {
        if (me != null) {
            if (parseUtil.isSignedUp()) {
                usernameEdit.setText(me.name());
                usernameEdit.setEnabled(false);
                emailEdit.setText(me.getEmail());
                emailEdit.setEnabled(false);
                passwordEdit.setText("********");
                passwordEdit.setEnabled(false);
            }
            // recieve photos that already needed to be resolved - no progress neeeded
            int length = imageViews.length;
            me.resolveSecondaryPhotos(DummyProgressCallback.creator(length),
                                      BitmapsToViews.createArray(imageViews, length));
            me.resolvePrimaryPhoto(DummyProgressCallback.creator(1), new BitmapsToViews(primaryImage));
        }
    }

    private static void setImageViews(ImageView[] imageViews, ImageView primaryImage) {
        selectedImages = new boolean[]{false, false, false};
        primarySelected = false;
        for (int i = 0; i < selectedImages.length; i++) {
            ImageView imageView = imageViews[i];
            final int finalI = i;
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastRequstedImageView = (ImageView)v;
                    lastImageIndex = finalI;
                    photoRequestHandler.sendEmptyMessage(
                        TripWithMeMain.REQUEST_PHOTO_HANDLER); // send user to another activity to pick a pic
                }
            });
        }
        primaryImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                lastRequstedImageView = (ImageView)v;
                lastImageIndex = PRIMARY_PHOTO_INDEX;
                photoRequestHandler.sendEmptyMessage(
                    TripWithMeMain.REQUEST_PHOTO_HANDLER); // send user to another activity to pick a pic
            }
        });
    }

    private static void setLoginButton(final Context context, final ParseUtil parseUtil,
                                       final PersonVisibleData me, final Dialog rootDialog) {
        final Button loginbtn = (Button)rootDialog.findViewById(id.loginanotherbtn);
        loginbtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog loginDialog = new Dialog(context);
                loginDialog.setContentView(layout.logindialog);
                EditText userNameLogin = (EditText)loginDialog.findViewById(id.loginusername);
                EditText userPasswordLogin = (EditText)loginDialog.findViewById(id.loginpassword);
                final EditText typeEmailForgot = (EditText)loginDialog.findViewById(id.emailforgotpass);
                final Button loginNowBtn = setLoginNowButton(loginDialog, userNameLogin, userPasswordLogin,
                                                             context, parseUtil, me, rootDialog, typeEmailForgot);
                TextView forgot = (TextView)loginDialog.findViewById(id.forgotlogintxt);
                forgot.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        typeEmailForgot.setVisibility(View.VISIBLE);
                        loginNowBtn.setTag(FORGOT_PASS_STATE);
                        loginNowBtn.setText("Send Reminder");
                    }
                });
                loginDialog.show();
            }
        });
    }

    private static Button setLoginNowButton(final Dialog loginDialog, final EditText userNameLogin,
                                            final EditText userPasswordLogin, final Context context,
                                            final ParseUtil parseUtil, final PersonVisibleData me,
                                            final Dialog homeDialog, final EditText typeEmailForgot) {
        final Button loginNow = (Button)loginDialog.findViewById(id.loginnow);
        loginNow.setTag(REGULAR_LOGIN);
        loginNow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int tagVal = (Integer)loginNow.getTag();
                if (tagVal == REGULAR_LOGIN) {
                    String name = userNameLogin.getText().toString();
                    String password = userPasswordLogin.getText().toString();
                    if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                        Toast.makeText(context, "Cannot login w/o user name or password", Toast.LENGTH_SHORT)
                            .show();
                    } else {
                        Toast.makeText(context, "Login in backgroud...", Toast.LENGTH_SHORT).show();
                        parseUtil.loginAndUpdateMe(name, password, me, new ActionDoneCallback() {
                            @Override
                            public void done() {
                                loginDialog.dismiss();
                                homeDialog.dismiss();
                            }
                        });
                    }
                } else if (tagVal == FORGOT_PASS_STATE) {
                    String emailForReset = typeEmailForgot.getText().toString();
                    if (emailForReset != null && !emailForReset.isEmpty()) {
                        parseUtil.requestPasswordResetShowToast(emailForReset, new ActionDoneCallback() {
                            public void done() {
                                loginDialog.dismiss();
                                homeDialog.dismiss();
                            }
                        });
                    } else {
                        Toast.makeText(context, "Type a password please...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return loginNow;
    }


    private static void setUpdateButton(final Context context, final ParseUtil parseUtil,
                                        final PersonVisibleData me, final Dialog rootDialog,
                                        final EditText usernameEdit, final EditText passwordEdit,
                                        final EditText emailEdit, final ImageView[] imageViews,
                                        final ImageView primaryImage) {
        Button updatebtn = (Button)rootDialog.findViewById(id.updatebutton);
        final boolean isSignedUp = parseUtil.isSignedUp();
        if (isSignedUp) {
            updatebtn.setText("Update Me !");
        } else {
            updatebtn.setText("Sign Up !");
        }
        updatebtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                String email = emailEdit.getText().toString();
                if (username == null || username.isEmpty() || password == null || password.isEmpty() ||
                    email == null || email.isEmpty()) {
                    Toast.makeText(context, "Cannot continue w/o user name, password and email", Toast.LENGTH_SHORT)
                        .show();
                } else {
                    parseUtil.signupIfNeeded(me, username, password, email, new ActionDoneCallback() {
                        @Override
                        public void done() {
                            updateDataAfterSignup(imageViews, me, parseUtil, primaryImage);
                            Toast.makeText(context, "updating in backgroud...", Toast.LENGTH_SHORT).show();
                            rootDialog.dismiss();
                        }
                    });
                }
            }
        });
    }


    // TODO Do we need synchornize here?
    private static void updateDataAfterSignup(ImageView[] imageViews, PersonVisibleData me, ParseUtil parseUtil,
                                              ImageView primaryImage) {
        Bitmap[] bitmaps = new Bitmap[selectedImages.length];
        for (int i = 0; i < selectedImages.length; i++) {
            if (selectedImages[i]) {
                bitmaps[i] = ((BitmapDrawable)imageViews[i].getDrawable()).getBitmap();
            }
        }
        Bitmap primary = null;
        if (primarySelected) {
            primary = ((BitmapDrawable)primaryImage.getDrawable()).getBitmap();
        }
        parseUtil.updatePicsAndMe(primary, bitmaps, me);
    }

    public static void resultImageInUri(final Context context, final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null; // null will return as an error
                Message msg = new Message();
                msg.what = TripWithMeMain.RESOLVE_TO_BITMAP_HANDLER;
                try {
                    // get it
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                    // Resize to 100x100
                    if (bitmap != null) {
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        float scaleWidth = MAX_WIDTH_NEEDED_FOR_PIC / (float)width;
                        float scaleHeight = MAX_HEIGHT_NEEDED_FOR_PIC / (float)height;
                        // CREATE A MATRIX FOR THE MANIPULATION
                        Matrix matrix = new Matrix();
                        // RESIZE THE BIT MAP
                        matrix.postScale(scaleWidth, scaleHeight);
                        // "RECREATE" THE NEW BITMAP
                        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
                        if (resizedBitmap != null) {
                            bitmap = resizedBitmap;
                        }
                    }
                } catch (Exception e) {
                    Log.e("ImageUri", "Exception while fetch or recreate", e);
                }
                msg.obj = bitmap;
                photoRequestHandler.sendMessage(msg);
            }
        }).start();

    }

    public static void bitmapReceived(Context context, Bitmap bitmap) {
        if (bitmap != null) {
            lastRequstedImageView.setImageBitmap(bitmap);
            if (lastImageIndex != PRIMARY_PHOTO_INDEX) {
                selectedImages[lastImageIndex] = true;
            } else {
                primarySelected = true;
            }
        } else {
            Toast.makeText(context, "Error while getting pic... :(", Toast.LENGTH_SHORT);
        }
    }

    public interface ActionDoneCallback {

        void done();
    }
}
