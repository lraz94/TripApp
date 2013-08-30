package com.source.tripwithme.components;

public abstract class CollectionWithListeners<T> {


    public abstract void addListListener(ListenerOnCollection<T> listener);

    public abstract void add(T object);

    public abstract boolean remove(T object);

    public abstract void removeAll();

    public abstract int size();

}
