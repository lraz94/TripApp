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


    //public void openSearchOption(final ArrayList<PersonVisibleData> suggestions) {
    //    final Dialog dialog = new Dialog(this);
    //    dialog.setContentView(R.layout.searchlayout);
    //    final AutoCompleteTextView autoComplete = (AutoCompleteTextView)dialog.findViewById(R.id.autoComplete);
    //    ArrayAdapter<PersonVisibleData> adapter = new ArrayAdapter<PersonVisibleData>(this,
    //                                                                                  android.R.layout.simple_dropdown_item_1line,
    //                                                                                  suggestions);
    //    autoComplete.setAdapter(adapter);
    //    autoComplete.setThreshold(1);
    //    final TextView error = (TextView)dialog.findViewById(R.id.error);
    //    Button searchnow = (Button)dialog.findViewById(R.id.searchnow);
    //    searchnow.setOnClickListener(new OnClickListener() {
    //        public void onClick(View arg0) {
    //            error.setText("");
    //            InputMethodManager inputManager =
    //                (InputMethodManager)PeopleMenu.this.getSystemService(Context.INPUT_METHOD_SERVICE);
    //            inputManager.hideSoftInputFromWindow(dialog.getCurrentFocus().getWindowToken(),
    //                                                 InputMethodManager.HIDE_NOT_ALWAYS);
    //            String got = autoComplete.getText().toString();
    //            PersonVisibleData candid;
    //            for (PersonVisibleData suggestion : suggestions) {
    //                if ((candid = suggestion).getName().equals(got)) {
    //                    dialog.dismiss();
    //                    reactToPersonSearch(candid);
    //                    return;
    //                }
    //            }
    //            error.setText("Sorry, Person isn't found...");
    //        }
    //    });
    //    dialog.setTitle("Find person");
    //    dialog.setCancelable(true);
    //    dialog.show();
    //}
    //
    //public void reactToPersonSearch(PersonVisibleData person) {
    //    ListWithListeners<PersonVisibleData> refrence = _caller.getListReference();
    //    if (refrence.contains(person)) {
    //        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    //        alertDialog.setTitle(person.getName() + " Search");
    //        alertDialog.setMessage("Person searched is alreday in persons list");
    //        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
    //            public void onClick(DialogInterface dialog, int which) {
    //                alertDialog.dismiss();
    //            }
    //        });
    //        alertDialog.setIcon(R.drawable.findperson);
    //        alertDialog.show();
    //    } else {
    //        refrence.add(person);
    //    }
    //}

}
