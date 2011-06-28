package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TestUserAgent {

    @Test
    public void testFirefoxDetection() throws Exception {
        UserAgent ua = new UserAgent("Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.9.0.15) Gecko/2009101601 Firefox 2.1 (.NET CLR 3.5.30729)");
        assertEquals("firefox 2 1 desktop", ua.toString());
    }
    
}
