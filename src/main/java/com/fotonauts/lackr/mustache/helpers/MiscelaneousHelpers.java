package com.fotonauts.lackr.mustache.helpers;

import com.github.jknack.handlebars.Options;

public class MiscelaneousHelpers {

    public static CharSequence humanize_integer(Object numberAsObject, Options options) {
        if(numberAsObject == null)
            return "";
        if (numberAsObject instanceof Number) {
            long n = ((Number) numberAsObject).longValue();
            if (n >= 10_000_000) {
                return (n / 1_000_000) + "M";
            } else if (n >= 10_000) {
                return (n / 1_000) + "k";
            }
        }
        return numberAsObject.toString();
    }

}
