package com.source.tripwithme.visible_data;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.source.tripwithme.R;
import com.source.tripwithme.R.id;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.SocialNetwork;
import com.source.tripwithme.databases.OfflineMessagesUtil;
import com.source.tripwithme.images_resolve.AsyncTaskResolver;
import com.source.tripwithme.images_resolve.AsyncTaskResolver.PostExecuter;
import com.source.tripwithme.images_resolve.BitmapsToViews;
import com.source.tripwithme.images_resolve.DummyProgressCallback;

public class PersonDialogCreator {

    private final PersonVisibleData personVisibleData;
    private TextView historyTextView;

    public PersonDialogCreator(PersonVisibleData personVisibleData) {
        this.personVisibleData = personVisibleData;
    }

    public void refreshHistoryTextIfCan() {
        if (historyTextView != null && historyTextView.isShown()) {
            historyTextView.setText(personVisibleData.getSpannedHistory(), TextView.BufferType.SPANNABLE);
            historyTextView.invalidate();
        }
    }

    public void paintDialog(final TripWithMeMain activity, final PersonVisibleData forUser) {
        new AsyncTaskResolver(activity, new PostExecuter() {
            @Override
            public void postExecute() {
                showDialog(activity, forUser);
            }
        }, personVisibleData).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showDialog(TripWithMeMain activity, PersonVisibleData forUser) {
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.baloonlayout);
        putPicsOnDialog(dialog);
        putSocialNetsOnDialog(dialog);
        putStatusTextOnDialog(dialog, forUser);
        putCountryOnDialog(dialog);
        putButtonsWithDismissOnDialog(dialog, activity, forUser);
        // some dialog perparations
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.setCancelable(true);
        dialog.setTitle(personVisibleData.name());
        dialog.show();
    }


    private void putPicsOnDialog(Dialog dialog) {
        ImageView primaryImage = (ImageView)dialog.findViewById(id.primaryphotodialog);
        personVisibleData.resolvePrimaryPhoto(DummyProgressCallback.creator(1), new BitmapsToViews(primaryImage));
        int[] imagesId = {R.id.img1, R.id.img2, R.id.img3};
        int length = imagesId.length;

        ImageView[] imageViews = new ImageView[length];
        for (int i = 0; i < length; i++) {
            imageViews[i] = (ImageView)dialog.findViewById(imagesId[i]);
        }
        // recieve photos that already needed to be resolved - no progress neeeded
        personVisibleData.resolveSecondaryPhotos(DummyProgressCallback.creator(length),
                                                 BitmapsToViews.createArray(imageViews, length));
    }

    private void putSocialNetsOnDialog(Dialog dialog) {
        SocialNetwork socialNets[] = personVisibleData.getSocialNetworks();
        ImageView socialImage[] = {(ImageView)dialog.findViewById(R.id.socialbaloon1),
            (ImageView)dialog.findViewById(R.id.socialbaloon2), (ImageView)dialog.findViewById(R.id.socialbaloon3)};
        DataInsertToImageViews.fillImagesViewsWithData(socialImage, socialNets,
                                                       DataInsertToImageViews.NO_RESOUCE_IDENTIFIER);
    }

    private void putStatusTextOnDialog(Dialog dialog, PersonVisibleData forUser) {
        TextView statustxt = (TextView)dialog.findViewById(R.id.statushere);
        String statusStr = personVisibleData.statusString();
        if (statusStr == null) {
            statusStr = "";
        }
        String addings = "";
        if (personVisibleData.isCheckedIn()) {
            if (!forUser.isCheckedIn()) {
                addings = " - check in to chat!";
            } else {
                addings = " - you can now chat! :)";
            }
        } else {
            if (!forUser.isCheckedIn()) {
                addings = " - check in to leave a message! :)";
            } else {
                addings = " - unavailable for chat - leave a message!";
            }
        }
        SpannableString content = new SpannableString("Status: " + statusStr + addings);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        statustxt.setText(content);
    }

    private void putCountryOnDialog(Dialog dialog) {
        TextView countryText = (TextView)dialog.findViewById(id.countryhere);
        Country primaryCountry = personVisibleData.getCountry();
        if (primaryCountry != null) {
            countryText.setText("Country: " + primaryCountry.getFullName());
        }
    }

    private void putButtonsWithDismissOnDialog(final Dialog dialog, final TripWithMeMain activity,
                                               final PersonVisibleData forUser) {
        Button byebut = (Button)dialog.findViewById(R.id.byeBubble);
        byebut.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        Button rembut = (Button)dialog.findViewById(R.id.remove);
        rembut.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                personVisibleData.remove();
                dialog.dismiss();
            }
        });
        final Button buttonIm = (Button)dialog.findViewById(id.clickImButton);
        if (!forUser.isCheckedIn()) {
            buttonIm.setEnabled(false);
        }
        buttonIm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Chat with " + personVisibleData.name());
                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                historyTextView = new TextView(activity);
                historyTextView.setSingleLine(false);
                historyTextView.setText(personVisibleData.getSpannedHistory(), TextView.BufferType.SPANNABLE);
                final EditText editText = new EditText(activity);
                Button button = new Button(activity);
                button.setText("Send!");
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Editable editable = editText.getText();
                        if (editable != null) {
                            String typed = editable.toString().trim();
                            if (!typed.isEmpty()) {
                                if (activity.signUpIsMissing()) {
                                    activity.showSignupUpdateDialog(true);
                                } else {
                                    editText.setText("");
                                    OfflineMessagesUtil.addMessageToUserInBackground(
                                        personVisibleData.getUniqueChatID(), typed, forUser.getUniqueChatID());
                                    personVisibleData.concatSpannableMessage(Html.fromHtml("<b>You:</b> " + typed));
                                    refreshHistoryTextIfCan();
                                }
                            }
                        }
                    }
                });
                button.setEnabled(buttonIm.isEnabled());
                layout.addView(historyTextView);
                layout.addView(editText);
                layout.addView(button);
                builder.setView(layout);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }
}