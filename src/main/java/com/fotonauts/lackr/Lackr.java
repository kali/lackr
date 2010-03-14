package com.fotonauts.lackr;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Log4jConfigurer;

public class Lackr {

    public static void main(String[] args) throws Exception {
        Log4jConfigurer.initLogging("classpath:log4j.properties");

        Logger log = LoggerFactory.getLogger(Lackr.class);

        ApplicationContext ctx = new ClassPathXmlApplicationContext("lackr.xml");

        Server server = (Server) ctx.getBean("Server");
        server.start();
        log.info("jetty server started");
        server.join();
    }
}
