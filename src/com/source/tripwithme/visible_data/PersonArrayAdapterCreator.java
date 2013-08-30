package com.source.tripwithme.visible_data;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.source.tripwithme.R;
import com.source.tripwithme.R.drawable;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.SocialNetwork;
import com.source.tripwithme.images_resolve.BitmapsToViews;
import com.source.tripwithme.images_resolve.DummyProgressCallback;
import com.source.tripwithme.images_resolve.ProgressCallbackCreator;

import java.text.DecimalFormat;

public class PersonArrayAdapterCreator {

    private final PersonVisibleData person;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public PersonArrayAdapterCreator(PersonVisibleData personVisibleData) {
        this.person = personVisibleData;
    }

    public void convertView(View toConvertView) {
        setTextViews(toConvertView);
        setImageViews(toConvertView);
    }

    private void setImageViews(View convertView) {
        final ImageView photoImage = (ImageView)convertView.findViewById(R.id.avatar);
        ImageView primCountryIco = (ImageView)convertView.findViewById(R.id.imageLang1);
        ImageView socialNetsImgs[] = {(ImageView)convertView.findViewById(R.id.social1),
            (ImageView)convertView.findViewById(R.id.social2), (ImageView)convertView.findViewById(R.id.social3)};
        // social net will be set in fillImagesViewsWithData
        person.resolvePrimaryPhoto(new ProgressCallbackCreator(new DummyProgressCallback(), 1),
                                   new BitmapsToViews(photoImage, drawable.ic_launcher));
        Country primary = person.getCountry();
        if (primary != null) {
            primCountryIco.setImageResource(primary.resource());
        } else {
            primCountryIco.setImageResource(R.drawable.un);
        }
        SocialNetwork[] socialNets = person.getSocialNetworks();
        DataInsertToImageViews
            .fillImagesViewsWithData(socialNetsImgs, socialNets, DataInsertToImageViews.NO_RESOUCE_IDENTIFIER);

    }

    private void setTextViews(View convertView) {
        TextView username = (TextView)convertView.findViewById(R.id.username);
        TextView status = (TextView)convertView.findViewById(R.id.status);
        if (username != null) {
            username
                .setText(person.name() + " (" + DECIMAL_FORMAT.format(person.distanceFromUser()) + " Kms)");
        }

        if (status != null) {
            status.setText("Status: " + person.statusString());
        }


    }

}
