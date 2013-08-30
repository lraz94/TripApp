package com.source.tripwithme.components;

public abstract class ResourceFlagedItem {

    private final String name;
    private final int resource;

    public ResourceFlagedItem(String name, int resource) {
        this.name = name;
        this.resource = resource;
    }

    public String name() {
        return name;
    }

    public int resource() {
        return resource;
    }

}