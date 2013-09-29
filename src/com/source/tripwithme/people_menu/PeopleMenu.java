package com.source.tripwithme.people_menu;

import android.R.style;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.source.tripwithme.R;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.layout;
import com.source.tripwithme.R.string;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.util.concurrent.atomic.AtomicBoolean;

public class PeopleMenu {

    private UserItemAdapter adapter;
    private TextView gettingData;
    private TextView titleTextView;
    private AtomicBoolean busy;

    private static String TITLE_BASIC_STRING; // considered constant


    public PeopleMenu(UserItemAdapter adapter, String titleBasicString) {
        this.adapter = adapter;
        adapter.setListenerToOngoingWork(new ListenerToOngoingWork());
        busy = new AtomicBoolean(false);
        TITLE_BASIC_STRING = titleBasicString;
    }

    public void showAsDialog(Context context) {
        final Dialog dialog = new Dialog(context, style.Theme_Holo_Dialog_NoActionBar);
        dialog.setContentView(layout.screentwo);
        final ListView listview = (ListView)dialog.findViewById(R.id.listActivePerson);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PersonVisibleData item = adapter.getItem(position);
                if (item != null) {
                    item.tap();
                }
                dialog.dismiss();
            }
        });
        Button done = (Button)dialog.findViewById(R.id.exitListActivePeopleButton);
        done.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        gettingData = (TextView)dialog.findViewById(id.updatestateTextView);
        if (busy.get()) {
            gettingData.setText(string.gettingdata);
        } else if (adapter.isEmpty()) {
            gettingData.setText(string.nofriends);
        } else {
            gettingData.setText(string.listisupdated);
        }
        titleTextView = (TextView)dialog.findViewById(id.menuTitleView);
        updateTitleByCount();
        dialog.show();
    }

    public class ListenerToOngoingWork {

        public void busy() {
            busy.set(true);
            if (gettingData != null) { // if menu not on the textView might be null
                gettingData.setText(string.gettingdata);
            }
        }

        public void free() {
            busy.set(false);
            if (gettingData != null) { // if menu not on the textView might null
                gettingData.setText(string.listisupdated);
            }
        }

        public void updateCount() {
            updateTitleByCount();
        }
    }

    private void updateTitleByCount() {
        if (titleTextView != null) {   // if menu not on the textView might null
            titleTextView.setText(TITLE_BASIC_STRING + " (" + adapter.getCount() + ")");
        }
    }
}
