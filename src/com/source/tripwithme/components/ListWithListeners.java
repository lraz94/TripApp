package com.source.tripwithme.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ListWithListeners first fire the event to all listeners and than update the delegator ArrayList itself.
 * This was created to bridge between ArrayLists of different types, which communicate with one another.
 * This class doesn't extends arrayList<T> in to force this API.
 */

public class ListWithListeners<T> extends CollectionWithListeners<T> {

    private List<T> delegator;
    private ArrayList<ListenerOnCollection<T>> listeners;

    public ListWithListeners() {
        delegator = Collections.synchronizedList(new ArrayList<T>());
        listeners = new ArrayList<ListenerOnCollection<T>>();
    }

    @Override
    public void addListListener(ListenerOnCollection<T> listener) {
        listeners.add(listener);
        notifyNewListener(listener);
    }

    private void notifyNewListener(ListenerOnCollection<T> listener) {
        for (T t : delegator) {
            listener.itemWasAdded(t);
        }
    }

    /*
        Add to the begging of the list.
     */
    @Override
    public void add(T object) {
        fireAllOfAdd(object);
        delegator.add(0, object);
    }

    private void fireAllOfAdd(T object) {
        for (ListenerOnCollection<T> listener : listeners) {
            listener.itemWasAdded(object);
        }
    }

    @Override
    public boolean remove(T object) {
        fireAllOfRemove(object);
        return delegator.remove(object);
    }

    private void fireAllOfRemove(T object) {
        for (ListenerOnCollection<T> listener : listeners) {
            listener.itemWasRemoved(object);
        }
    }

    //public boolean contains(T o) {
    //    return delegator.contains(o);
    //}

    @Override
    public void removeAll() {
        // sysnchronization issues
        List<T> backup = new ArrayList<T>(delegator);
        delegator.clear();
        for (T t : backup) {
            fireAllOfRemove(t);
        }
    }

    @Override
    public int size() {
        return delegator.size();
    }

    public T get(int i) {
        return delegator.get(i);
    }

}