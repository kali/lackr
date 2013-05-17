package com.fotonauts.lackr.mustache.helpers;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import com.github.jknack.handlebars.Options;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

public class DateTimeFormatterHelpers {

    private static Long extractTimestampMS(Object object) {
        if (object instanceof Number) {
            Number number = (Number) object;
            return number.longValue() * 1000;
        } else if (object instanceof Map) {
            @SuppressWarnings("rawtypes")
            Map hash = (Map) object;
            if (hash.containsKey("$DATE")) {
                if (hash.get("$DATE") instanceof Number) {
                    return ((Number) hash.get("$DATE")).longValue();
                } else {
                    throw new RuntimeException("expected $DATE to be a number, found: " + hash.get("$DATE"));
                }
            } else
                throw new RuntimeException("expected hash to contains $DATE, found: " + hash.toString());
        } else
            throw new RuntimeException("expected a date, found: " + object);
    }


    public static CharSequence relative_datetime(Object object, Options options) {
        if(object == null)
            return "";
        Long timestamp = extractTimestampMS(object);
        try {
            Locale locale = (Locale) options.context.get("_ftn_locale");
            return DurationFormat.getInstance(ULocale.forLocale(locale)).formatDurationFromNow(
                    timestamp.longValue() - System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println(e);
            throw e;
        }
    }

    // options:
    // 'format' : 'time' / 'date' / 'date_time'
    // 'type' : 'short' / 'medium' / 'long' / 'full'
    public static CharSequence absolute_datetime(Object date, Options options) {
        if(date == null)
            return "";
        return icuFormatDateTime(extractTimestampMS(date), options, false);
    }

    private static CharSequence icuFormatDateTime(Long timestampMS, Options options, boolean relative) {
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

        return df.format(new Date(timestampMS));
    }
}
