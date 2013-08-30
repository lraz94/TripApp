package com.source.tripwithme.components;

public interface ListenerOnCollection<T> {

    public void itemWasAdded(T object);

    public void itemWasRemoved(T obj);

}
