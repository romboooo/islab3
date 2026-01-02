package com.example;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class JAXRSConfiguration extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(com.example.rest.DragonResource.class);
        classes.add(com.example.rest.PersonResource.class);
        classes.add(com.example.rest.ImportResource.class);
        classes.add(com.example.HelloResource.class);

        classes.add(MultiPartFeature.class);

        return classes;
    }
}