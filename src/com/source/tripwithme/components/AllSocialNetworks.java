package com.source.tripwithme.components;

import com.source.tripwithme.R;

public class AllSocialNetworks {

    private static final String FACEBOOK_STRING = "facebook";
    private static final String GPLUS_STRING = "gplus";
    private static final String TWITTER_STRING = "twitter";
    private static SocialNetwork FACEBOOK = null;
    private static SocialNetwork TWITTER = null;
    private static SocialNetwork GPLUS = null;


    public static SocialNetwork getSocialNetwork(String name) {
        if (name == null) {
            return defaultSocialNetwork();
        }
        if (name.equalsIgnoreCase(FACEBOOK_STRING)) {
            return facebook();
        } else if (name.equalsIgnoreCase(GPLUS_STRING)) {
            return gplus();
        } else if (name.equalsIgnoreCase(TWITTER_STRING)) {
            return twitter();
        } else {
            return defaultSocialNetwork();
        }
    }

    private static SocialNetwork defaultSocialNetwork() {
        return facebook();
    }

    public static SocialNetwork facebook() {
        if (FACEBOOK == null) {
            FACEBOOK = new SocialNetwork(FACEBOOK_STRING, R.drawable.facebook);
        }
        return FACEBOOK;
    }

    public static SocialNetwork gplus() {
        if (GPLUS == null) {
            GPLUS = new SocialNetwork(GPLUS_STRING, R.drawable.gplus);
        }
        return GPLUS;
    }

    public static SocialNetwork twitter() {
        if (TWITTER == null) {
            TWITTER = new SocialNetwork(TWITTER_STRING, R.drawable.twitter);
        }
        return TWITTER;
    }

}
