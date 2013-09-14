package com.source.tripwithme.people_menu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.source.tripwithme.R;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.layout;
import com.source.tripwithme.R.string;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.util.concurrent.atomic.AtomicBoolean;

public class PeopleMenu extends Activity {

    private static UserItemAdapter adapter;
    private static TextView gettingData;
    private static TextView titleTextView;
    private static AtomicBoolean busy;

    private static String TITLE_BASIC_STRING; // considered constant

    // need empty constructor for activity...
    @SuppressWarnings("UnusedDeclaration")
    public PeopleMenu() {
        super();
    }

    public PeopleMenu(UserItemAdapter adapter, String titleBasicString) {
        PeopleMenu.adapter = adapter;
        adapter.setListenerToOngoingWork(new ListenerToOngoingWork());
        busy = new AtomicBoolean(false);
        TITLE_BASIC_STRING = titleBasicString;
    }

    public void onCreate(Bundle unused) {
        super.onCreate(unused);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layout.screentwo);
        final ListView modeList = (ListView)findViewById(R.id.listActivePerson);
        registerForContextMenu(modeList);
        modeList.setAdapter(adapter);
        modeList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PersonVisibleData item = adapter.getItem(position);
                if (item != null) {
                    item.tap();
                }
                startActivity(new Intent(PeopleMenu.this, TripWithMeMain.class));
            }
        });
        Button done = (Button)findViewById(R.id.exitListActivePeopleButton);
        done.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(PeopleMenu.this, TripWithMeMain.class));
            }
        });
        gettingData = (TextView)findViewById(id.updatestateTextView);
        if (busy.get()) {
            gettingData.setText(string.gettingdata);
        } else if (adapter.isEmpty()) {
            gettingData.setText(string.nofriends);
        } else {
            gettingData.setText(string.listisupdated);
        }
        titleTextView = (TextView)findViewById(id.menuTitleView);
        updateTitleByCount();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        getMenuInflater().inflate(R.menu.contextsecondscreen, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        PersonVisibleData rem = adapter.getItem(info.position);
        if (rem != null) {
            rem.remove(); // will indirectly call list that will call itemWasRemoved
        }
        return true;
    }

    public class ListenerToOngoingWork {

        public void busy() {
            busy.set(true);
            if (gettingData != null) { // if menu not on the textView is null
                gettingData.setText(string.gettingdata);
            }
        }

        public void free() {
            busy.set(false);
            if (gettingData != null) { // if menu not on the textView is null
                gettingData.setText(string.listisupdated);
            }
        }

        public void updateCount() {
            updateTitleByCount();
        }
    }

    private void updateTitleByCount() {
        if (titleTextView != null) {
            titleTextView.setText(TITLE_BASIC_STRING + " (" + adapter.getCount() + ")");
        }
    }
}
