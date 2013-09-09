package com.source.tripwithme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.source.tripwithme.R.id;
import com.source.tripwithme.components.TreeSetWithListeners;
import com.source.tripwithme.databases.OfflineMessagesUtil;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessagesHelper {


    private static final long SLEEP_BETWEEN_MESSAGES_MILLIS = 1000; // 2 sec

    private static final int ADD_PERSON_TO_LIST_HANDLER = 550;
    private static final int REFRESH_BUTTON_AND_HISTORY_HANDLER = 660;
    private static final String MESSAGES_TAG = "MessagesHandling";

    private final TripWithMeMain caller;

    private final TreeSetWithListeners<PersonVisibleData> people;

    private final Handler refreshingHandler;

    private List<PersonMessagePair> messages;

    private Button messageButton;

    public MessagesHelper(TripWithMeMain tripWithMeMain, TreeSetWithListeners<PersonVisibleData> people) {
        caller = tripWithMeMain;
        this.people = people;
        refreshingHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ADD_PERSON_TO_LIST_HANDLER) {
                    PersonVisibleData person = (PersonVisibleData)msg.obj;
                    MessagesHelper.this.people.add(person);
                } else if (msg.what == REFRESH_BUTTON_AND_HISTORY_HANDLER) {
                    refreshMessaegsAndGlow();
                    PersonVisibleData person = (PersonVisibleData)msg.obj;
                    person.refreashHistoryText();
                }

            }
        };
    }

    public void start() {
        messages = Collections.synchronizedList(new ArrayList<PersonMessagePair>());
        startMessagesGui();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!caller.isFinishing()) {
                        String uniqueID = caller.getUserUniqueID();
                        if (uniqueID != null) {
                            List<String> pendingMessages =
                                OfflineMessagesUtil.getPendingMessagesBlock(uniqueID);
                            if (!pendingMessages.isEmpty()) {
                                Log.d(MESSAGES_TAG, "new messages: " + pendingMessages);
                                addAllNewMessagesBlock(pendingMessages);
                            }
                        }
                        Thread.sleep(SLEEP_BETWEEN_MESSAGES_MILLIS);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addAllNewMessagesBlock(List<String> pendingMessages) {
        for (String msg : pendingMessages) {
            String splits[] = OfflineMessagesUtil.splitToMessageTextAndUserID(msg);
            String personID = splits[1];
            String actualMessage = splits[0];
            // serach in already sent messages won't help since person might be deleted already...
            // so we seach only on active people list
            PersonVisibleData person = people.getMatched(PersonVisibleData.minimalPersonByID(personID));
            if (person == null) {
                // need to fetch from server...
                person = caller.getPersonFromServerBlock(personID);
            }
            if (person != null) {
                createNewMessgeRefresh(actualMessage, person);
            }
        }
    }

    private void createNewMessgeRefresh(String actualMessage, PersonVisibleData person) {
        PersonMessagePair newPair = new PersonMessagePair(person, actualMessage);
        person.concatSpannableMessage(newPair.createSpannableMessage());
        messages.add(newPair);
        handlerRefreshButtonsHistory(person);
    }


    private void startMessagesGui() {
        messageButton = (Button)caller.findViewById(id.messageButton);
        messageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(caller);
                builder.setTitle("Jump to person by message");
                int size = messages.size();
                final String[] messagesArray = new String[size];
                for (int i = 0; i < size; i++) {
                    PersonMessagePair pair = messages.get(i);
                    messagesArray[i] = pair.person.name() + ": " + pair.msg;
                }
                builder.setItems(messagesArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int item) {
                        // must validate sizes since click is asynchronous
                        if (item >= 0 && item < messages.size()) {
                            PersonMessagePair pair = messages.remove(item);
                            if (pair != null) {
                                // add to people if needed
                                addPersonIfNeededBackground(pair.person);

                                // refresh button - we're on UI already
                                refreshMessagesButton();

                                // finally tap him !
                                pair.person.tap();
                                return;
                            }
                        }
                        Toast.makeText(caller, "Person is already gone...", Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });
    }

    /*
        hope that when the user is finally added with all log(n) operations it is already tapped
        and its pic we show after fetching...
     */
    private void addPersonIfNeededBackground(final PersonVisibleData person) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (people.getMatched(PersonVisibleData.minimalPersonByID(person.getUniqueChatID())) == null) {
                    handlerAddPersonToList(person);
                }
            }
        }).start();

    }


    private static class PersonMessagePair {

        private final PersonVisibleData person;
        private final String msg;

        public PersonMessagePair(PersonVisibleData person, String msg) {
            this.person = person;
            this.msg = msg;
        }

        public Spanned createSpannableMessage() {
            return Html.fromHtml("<b>" + person.name() + ":</b> " + msg);
        }

    }

    private void refreshMessagesButton() {
        messageButton.setText("Messages (" + messages.size() + ")");
    }

    private void refreshMessaegsAndGlow() {
        refreshMessagesButton();
        ButtonEffect.glowOnUIThread(messageButton);
    }

    private void handlerRefreshButtonsHistory(PersonVisibleData person) {
        Message guiMessage = new Message();
        guiMessage.what = REFRESH_BUTTON_AND_HISTORY_HANDLER;
        guiMessage.obj = person;
        refreshingHandler.sendMessage(guiMessage);
    }

    private void handlerAddPersonToList(PersonVisibleData person) {
        Log.d(MESSAGES_TAG, "MESSAGES - new person is added to list");
        Message guiMessage = new Message();
        guiMessage.what = ADD_PERSON_TO_LIST_HANDLER;
        guiMessage.obj = person;
        refreshingHandler.sendMessage(guiMessage);
    }
}
