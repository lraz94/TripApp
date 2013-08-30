package com.source.tripwithme.visible_data;

import android.view.View;
import com.parse.ProgressCallback;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.images_resolve.BitmapGotCallback;
import com.source.tripwithme.images_resolve.DummyBitmapCallback;
import com.source.tripwithme.images_resolve.ImageResolver;
import com.source.tripwithme.images_resolve.ProgressCallbackCreator;


public abstract class BaseVisibleData {

    private final String status;
    private final RemoverCallback remover;
    private final TappedCallback tapper;
    protected PointWithDistance address;
    protected String name;
    protected ImageResolver primaryPhoto;
    protected ImageResolver[] secondaryPhotos;

    public BaseVisibleData(String name, PointWithDistance address, String status, ImageResolver primaryPhoto,
                           ImageResolver[] secondaryPhotos, RemoverCallback remover, TappedCallback tapper) {
        this.name = name;
        this.address = address;
        this.status = status;
        this.primaryPhoto = primaryPhoto;
        this.secondaryPhotos = secondaryPhotos;
        this.remover = remover;
        this.tapper = tapper;
    }

    public String name() {
        return name;
    }

    public PointWithDistance address() {
        return address;
    }

    public String statusString() {
        return status;
    }

    public abstract void paintActivePopup(TripWithMeMain activity, PersonVisibleData requester);

    public abstract void paintArrayAdapterView(View toConvertView);

    public void resolvePrimaryPhoto(ProgressCallbackCreator creator, BitmapGotCallback bitmapGotCallback) {
        ProgressCallback progressCallback = creator.spawnBasedOnKnowNumber();
        if (primaryPhoto != null) {
            primaryPhoto.resolve(progressCallback, bitmapGotCallback);
        } else {
            progressCallback.done(100);  // let the progress continue
            bitmapGotCallback.got(null); // best not to show at all?
        }
    }

    public void resolveSecondaryPhotos(ProgressCallbackCreator creator, BitmapGotCallback[] bitmapGotCallback) {
        if (secondaryPhotos != null) {
            int length = secondaryPhotos.length;
            for (int i = 0; i < length; i++) {
                ImageResolver secondary = secondaryPhotos[i];
                ProgressCallback progressCallback = creator.spawnBasedOnKnowNumber();
                if (secondary != null) {
                    secondary.resolve(progressCallback, bitmapGotCallback[i]);
                } else {
                    progressCallback.done(100);  // let the progress continue
                    bitmapGotCallback[i].got(null); // best not to show at all?!
                }
            }
        }
    }

    public ProgressCallbackCreator preResolveAll(ProgressCallback progressCallback) {
        int lengthOfSecondary = secondaryPhotos.length;
        // progress for secondaryPhotos + 1 for primary
        ProgressCallbackCreator callbackCreator = new ProgressCallbackCreator(progressCallback, lengthOfSecondary + 1);
        resolvePrimaryPhoto(callbackCreator, new DummyBitmapCallback());
        BitmapGotCallback[] dummyCallbacks = new BitmapGotCallback[lengthOfSecondary];
        for (int i = 0; i < lengthOfSecondary; i++) {
            dummyCallbacks[i] = new DummyBitmapCallback();
        }
        resolveSecondaryPhotos(callbackCreator, dummyCallbacks);
        return callbackCreator;
    }

    public double distanceFromUser() {
        return address.getDistance();
    }

    public void remove() {
        remover.removeMe(this);
    }

    public void tap() {
        tapper.onTap(this);
    }

}

