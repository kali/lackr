package com.fotonauts.lackr.mustache.helpers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Options;

public class ComparisonHelpers {

    static Logger log = LoggerFactory.getLogger(ComparisonHelpers.class);

    
    public static CharSequence is(Object contextAsObject, Options options) {
        Object comparisonTargetAsObject = options.params.length > 0 ? options.params[0] : null;
        Boolean noneIsNull = (comparisonTargetAsObject != null) && (contextAsObject != null);
        
        try {
            if (noneIsNull && contextAsObject.equals(comparisonTargetAsObject)) {
                return options.fn(contextAsObject);
            } else {
                return options.inverse(contextAsObject);
            }
        } catch (Throwable e) {
            log.debug("error while evaluating 'is' helper:", e);
            return "";
        }
    }    
}
