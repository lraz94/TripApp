package com.source.tripwithme.people_menu;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
import com.source.tripwithme.people_menu.PeopleMenu.ListenerToOngoingWork;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UserItemAdapter extends ArrayAdapter<PersonVisibleData> {

    private static final int SLEEP_TO_VALIDATE_END_MILIS = 100;
    private ListenerToOngoingWork listenerToOngoingWork;
    private AtomicInteger atomicInteger;

    public UserItemAdapter(Context context, List<PersonVisibleData> users) {
        super(context, android.R.layout.simple_list_item_1, users);
        atomicInteger = new AtomicInteger(0);
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
                publishStart();   // end will be from adapter itself
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressCallbackCreator creator = new ProgressCallbackCreator(new DummyProgressCallback(), 1);
                        person.resolvePrimaryPhoto(creator, new DummyBitmapCallback());
                        try {
                            // wait for progress to end - RISKY
                            while (!creator.isDone()) {
                                Thread.sleep(SLEEP_TO_VALIDATE_END_MILIS);
                            }
                            Message msg = new Message();
                            msg.what = TripWithMeMain.ADAPTER_NEW_PERSON_HANDLER;
                            msg.obj = person;
                            guiHandler.sendMessage(msg);
                        } catch (InterruptedException e) {
                            Log.e("UserItem", "Interupted in resolve", e);
                        }
                    }
                }).start();
            }

            @Override
            public void itemWasRemoved(PersonVisibleData obj) {
                remove(obj);
                updateFriendsCount();
            }
        };
    }

    public synchronized void publishEnd() {
        if (listenerToOngoingWork != null) {
            int count = atomicInteger.decrementAndGet();
            if (count == 0) { // turn to free
                listenerToOngoingWork.free();
            }
        }
    }

    private synchronized void publishStart() {
        if (listenerToOngoingWork != null) {
            int count = atomicInteger.incrementAndGet();
            if (count == 1) { // turn to busy
                listenerToOngoingWork.busy();
            }
        }
    }

    public void setListenerToOngoingWork(ListenerToOngoingWork listenerToOngoingWork) {
        this.listenerToOngoingWork = listenerToOngoingWork;


    }

    public void updateFriendsCount() {
        if (listenerToOngoingWork != null) {
            listenerToOngoingWork.updateCount();
        }

    }
}