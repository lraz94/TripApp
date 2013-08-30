package com.source.tripwithme.components;

public class LimitedListWithListeners<T> extends ListWithListeners<T> {

    private final int limit;

    public LimitedListWithListeners(int limit) {
        super();
        this.limit = limit;
    }

    @Override
    public void add(T object) {
        super.add(object);
        if (size() > limit) {
            System.out.println("size is bigger than limit, size = " + size());
            T removed = get(size() - 1);
            remove(removed);
            System.out.println("removed from end of list: " + removed);

        }
    }
}
