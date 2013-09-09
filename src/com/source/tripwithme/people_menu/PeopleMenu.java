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
import com.source.tripwithme.R;
import com.source.tripwithme.R.layout;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.visible_data.PersonVisibleData;

public class PeopleMenu extends Activity {

    private static UserItemAdapter adapter;

    // need empty constructor for activity...
    @SuppressWarnings("UnusedDeclaration")
    public PeopleMenu() {
        super();
    }

    public PeopleMenu(UserItemAdapter adapter) {
        PeopleMenu.adapter = adapter;
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

}
