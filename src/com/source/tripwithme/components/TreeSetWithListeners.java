package com.source.tripwithme.components;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

/*
    like ListWithListeners but TreeSet.
 */
public class TreeSetWithListeners<T extends Comparable<T>> extends CollectionWithListeners<T> {

    private final ArrayList<ListenerOnCollection<T>> listeners;
    private final SortedSet<T> delegator;

    public TreeSetWithListeners() {
        delegator = Collections.synchronizedSortedSet(new TreeSet<T>());
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
       will add somewhere and not to start...
     */
    @Override
    public void add(T object) {
        fireAllOfAdd(object);
        delegator.add(object);
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


    @Override
    public void removeAll() {
        // sysnchronization issues
        HashSet<T> backup = new HashSet<T>(delegator);
        delegator.clear();
        for (T t : backup) {
            fireAllOfRemove(t);
        }
    }

    @Override
    public int size() {
        return delegator.size();
    }

    public T getMatched(T other) {
        SortedSet<T> tailSet = delegator.tailSet(other);
        if (tailSet != null && !tailSet.isEmpty()) {
            T candid = tailSet.first();
            if (candid.equals(other)) {
                return candid;
            }
        }
        return null;
    }

}
