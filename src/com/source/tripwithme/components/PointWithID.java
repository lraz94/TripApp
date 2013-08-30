package com.source.tripwithme.components;

public class PointWithID {

    private final long id;
    private final PointWithDistance point;

    private PointWithID(PointWithDistance point, long id) {
        this.point = point;
        this.id = id;
    }

    public static PointWithID generate(PointWithDistance point) {
        return new PointWithID(point, System.currentTimeMillis());
    }

    public static PointWithID recreate(PointWithDistance point, long id) {
        return new PointWithID(point, id);
    }

    public PointWithDistance getPoint() {
        return point;
    }

    public long getId() {
        return id;
    }

    public boolean equals(Object o) {
        return o instanceof PointWithID && ((PointWithID)o).getId() == this.getId();
    }

    public String toString() {
        return getPoint().toString() + " id:" + getId();
    }
}
