package com.source.tripwithme.components;

import android.util.Log;

public class LimitedListWithListeners<T> extends ListWithListeners<T> {

    private static final String LIMTED_LIST_TAG = "LimitedList";
    private final int limit;

    public LimitedListWithListeners(int limit) {
        super();
        this.limit = limit;
    }

    @Override
    public void add(T object) {
        super.add(object);
        if (size() > limit) {
            Log.d(LIMTED_LIST_TAG, "size is bigger than limit, size = " + size());
            T removed = get(size() - 1);
            remove(removed);
            Log.d(LIMTED_LIST_TAG, "removed from end of list: " + removed);

        }
    }
}
