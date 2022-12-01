package org.acme.getting.started;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Path("/hello")
public class GreetingResource {
    private static final String PROPERTY_FILE = "hotrod-client.properties";
    private static String CACHE_NAME = "simple-cache";
    //    private static String JDG_HOST = "10.211.55.2";
    private static String JDG_HOST = "zeus.shared";
    private static int HOTROD_PORT = 11222;

    @Inject
    GreetingService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/{name}")
    public String greeting(String name) {
        return service.greeting(name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
//        return "hello";
        Properties properties = getJDGProps(PROPERTY_FILE);
        int loopNum = Integer.parseInt(properties.getProperty("loop.nums"));

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.withProperties(properties);
        builder.statistics()
                .enable()
                .jmxEnable()
                .remoteCache(CACHE_NAME)
                .configuration(
                        "<infinispan>" +
                                "<cache-container>" +
                                "<distributed-cache name=\"" + CACHE_NAME + "\">" +
                                "<encoding media-type=\"application/x-protostream\"/>" +
                                "</distributed-cache>" +
                                "</cache-container>" +
                                "</infinispan>"
                );
        /*
        builder.statistics().enable().jmxEnable()
                .addServer()
                .host(JDG_HOST)
                .port(HOTROD_PORT)
                .security()
                .authentication()
                .username("admin")
                .password("j.yoko31")
                .remoteCache(CACHE_NAME)
                .configuration(
                      "<infinispan>" +
                         "<cache-container>" +
                            "<distributed-cache name=\"" + CACHE_NAME + "\">" +
                               "<encoding media-type=\"application/x-protostream\"/>" +
                            "</distributed-cache>" +
                         "</cache-container>" +
                      "</infinispan>"
                );
         */

        // Connect to the server
        //RemoteCacheManager cacheManager = Infinispan.connect();
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());

        // Obtain the remote cache
        RemoteCache<String, String> cache = cacheManager.getCache(CACHE_NAME);

        for(int i = 0; i < loopNum; i++){
            //String key = String.format("KEY-%05d",0);
            String key = String.format("KEY-%05d",i);
            String value = String.format("VAL-%05d", i);
            // Store a value
            cache.put(key, value);
            // Retrieve the value and print it out
            System.out.printf(key + " = %s\n", cache.get(key));
        }
        try {
            Thread.sleep(1*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /* remove data
        for(int i = 0; i < loopNum; i++){
            String key = String.format("KEY-%05d",i);
            String value = String.format("VAL-%05d", i);
            cache.remove(key);
            System.out.printf(key + " = %s\n", cache.get(key));
        }
        */

        //cacheManager.administration().removeCache();

        // Stop the cache manager and release all resources
        cacheManager.close();
        //cacheManager.stop();

        return "Finished.";
    }

    public static Properties getJDGProps(String prop_file_name) {
        try {
            InputStream in = GreetingResource.class.getClassLoader().getResourceAsStream(prop_file_name);
            if(in == null) {
                throw new IOException("No property file: " + prop_file_name);
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}