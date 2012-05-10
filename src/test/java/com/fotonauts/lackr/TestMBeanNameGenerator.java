package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fotonauts.lackr.hashring.RingHost;

public class TestMBeanNameGenerator {
    
    @Test
    public void testMBeanNameForHttpDirection() {
        assertEquals("v01-8000", RingHost.getMBeanNameFromUrlPrefix("http://v01.prod.fotonauts.net:8000"));
        assertEquals("localhost-8000", RingHost.getMBeanNameFromUrlPrefix("http://localhost:8000"));
    }
}
