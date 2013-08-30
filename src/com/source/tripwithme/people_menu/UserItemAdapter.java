package com.source.tripwithme.people_menu;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.source.tripwithme.R;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.ListenerOnCollection;
import com.source.tripwithme.images_resolve.DummyBitmapCallback;
import com.source.tripwithme.images_resolve.DummyProgressCallback;
import com.source.tripwithme.images_resolve.ProgressCallbackCreator;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.util.List;

public class UserItemAdapter extends ArrayAdapter<PersonVisibleData> {

    private static final int SLEEP_TO_VALIDATE_END_MILIS = 100;

    public UserItemAdapter(Context context, List<PersonVisibleData> users) {
        super(context, android.R.layout.simple_list_item_1, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        // run over last convertView
        convertView = inflater.inflate(R.layout.listitem, null);
        PersonVisibleData person = getItem(position);
        if (person != null) {
            person.paintArrayAdapterView(convertView);
        }
        return convertView;
    }


    public ListenerOnCollection<PersonVisibleData> getNewListener(final Handler guiHandler) {
        return new ListenerOnCollection<PersonVisibleData>() {
            @Override
            public void itemWasAdded(final PersonVisibleData person) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressCallbackCreator creator = new ProgressCallbackCreator(new DummyProgressCallback(), 1);
                        person.resolvePrimaryPhoto(creator, new DummyBitmapCallback());
                        try {
                            // wait for progress to end TODO RISKY
                            while (!creator.isDone()) {
                                Thread.sleep(SLEEP_TO_VALIDATE_END_MILIS);
                            }
                            Message msg = new Message();
                            msg.what = TripWithMeMain.ADAPTER_NEW_PERSON_HANDLER;
                            msg.obj = person;
                            guiHandler.sendMessage(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void itemWasRemoved(PersonVisibleData obj) {
                remove(obj);
            }
        };
    }
}