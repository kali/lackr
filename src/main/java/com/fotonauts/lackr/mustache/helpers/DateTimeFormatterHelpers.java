package com.fotonauts.lackr.mustache.helpers;

import java.util.Date;
import java.util.Locale;

import com.github.jknack.handlebars.Options;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

public class DateTimeFormatterHelpers {

    public static CharSequence relative_datetime(Number timestamp, Options options) {
        try {
            Locale locale = (Locale) options.context.get("_ftn_locale");
            return DurationFormat.getInstance(ULocale.forLocale(locale)).formatDurationFromNow(
                    timestamp.longValue() * 1000 - System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println(e);
            throw e;
        }
    }

    // options:
    // 'format' : 'time' / 'date' / 'date_time'
    // 'type' : 'short' / 'medium' / 'long' / 'full'
    public static CharSequence absolute_datetime(Number timestamp, Options options) {
        return icuFormatDateTime(timestamp, options, false);
    }

    private static CharSequence icuFormatDateTime(Number timestamp, Options options, boolean relative) {
        String format = options.hash("format", "time");
        String type = options.hash("type", "full");
        Locale locale = (Locale) options.context.get("_ftn_locale");

        int icuFormat;
        switch (type) {
        case "short":
            icuFormat = DateFormat.SHORT;
            break;
        case "medium":
            icuFormat = DateFormat.MEDIUM;
            break;
        case "long":
            icuFormat = DateFormat.LONG;
            break;
        default:
            icuFormat = DateFormat.FULL;
        }

        DateFormat df;
        switch (format) {
        case "date":
            df = DateFormat.getDateInstance(icuFormat, locale);
            break;
        case "date_time":
            df = DateFormat.getDateTimeInstance(icuFormat, icuFormat, locale);
            break;
        default:
            df = DateFormat.getTimeInstance(icuFormat, locale);
            break;
        }

        return df.format(new Date(timestamp.longValue() * 1000));
    }
}
