package com.source.tripwithme.databases;


import android.util.Log;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class OfflineMessagesUtil {

    private static final String PARSE_OBJECT_MSGS_CLASS = "pendingMessages";
    private static final String TO_USER_ID_KEY = "forUser";
    private static final String MESSAGES_KEY = "messages";
    private static final String PERSON_ID_SUFFIX_SEPERATOR = "@";

    private static final String OFFLINE_MSG_TAG = "OfflineMessages";

    public static List<String> getPendingMessagesBlock(String toUserID) {
        List<String> pendingMsgs = new ArrayList<String>();
        try {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(PARSE_OBJECT_MSGS_CLASS)
                .whereEqualTo(TO_USER_ID_KEY, toUserID);
            List<ParseObject> list = query.find();
            if (list != null && list.size() > 0) {
                ParseObject current = list.get(0);
                if (current != null) {
                    List<Object> objectList = current.getList(MESSAGES_KEY);
                    if (objectList != null) {
                        for (Object o : objectList) {
                            pendingMsgs.add(o.toString());
                        }
                        current.removeAll(MESSAGES_KEY, objectList);
                        current.save();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(OFFLINE_MSG_TAG, "pending", e);
        }
        return pendingMsgs;
    }


    public static void addMessageToUserInBackground(final String toUserID, final String msg, final String fromUserID) {
        // fetch if user is already known
        ParseQuery<ParseObject> query = ParseQuery.getQuery(PARSE_OBJECT_MSGS_CLASS)
            .whereEqualTo(TO_USER_ID_KEY, toUserID);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(final List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ParseObject targetParseObject;
                                if (parseObjects.size() == 0) {
                                    targetParseObject = new ParseObject(PARSE_OBJECT_MSGS_CLASS);
                                    ParseACL acl = new ParseACL();
                                    acl.setPublicWriteAccess(true); // everybody can write to everybody a message...
                                    acl.setPublicReadAccess(true);
                                    targetParseObject.setACL(acl);
                                    JSONArray array = new JSONArray();
                                    array.put(createMessage(msg, fromUserID));
                                    targetParseObject.put(TO_USER_ID_KEY, toUserID);
                                    targetParseObject.put(MESSAGES_KEY, array);

                                } else {
                                    targetParseObject = parseObjects.get(0);
                                    targetParseObject.add(MESSAGES_KEY, createMessage(msg, fromUserID));
                                }
                                targetParseObject.save();
                            } catch (Exception error) {
                                Log.e(OFFLINE_MSG_TAG, "Failure", error);
                            }
                        }
                    }).start();
                } else {
                    Log.e(OFFLINE_MSG_TAG, "Failure", e);
                }
            }
        }
        );
    }


    private static String createMessage(String msg, String fromUserID) {
        return msg + PERSON_ID_SUFFIX_SEPERATOR + fromUserID; // user id must not have @ as we separate by last index
    }

    // Message is first and id is last
    public static String[] splitToMessageTextAndUserID(String msg) {
        String[] userMsg = new String[2];
        int ind = msg.lastIndexOf(PERSON_ID_SUFFIX_SEPERATOR);
        int length = msg.length();
        if (ind == -1 || ind == length - 1) {
            userMsg[0] = "";
            userMsg[1] = "";
        } else {
            userMsg[0] = msg.substring(0, ind);
            userMsg[1] = msg.substring(ind + 1, length);
        }
        return userMsg;
    }

}
