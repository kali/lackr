package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fotonauts.lackr.picorassets.PicorAssetResolver;

public class TestAssetResolver {

    //@formatter:off 
    /*
    /var/folders/ld/4pf6jpb51vj9dnt0ywxg93y40000gn/T/junit4400314131069484551
    └── javascripts
        └── reporter
            ├── some.js
            |   ├── 4212.js
            |   └── some.js -> /var/folders/ld/4pf6jpb51vj9dnt0ywxg93y40000gn/T/junit4400314131069484551/javascripts/reporter/some.js/4212.js
            └── unversioned.js
                 
    */
    
    @Rule
    public TemporaryFolder root = new TemporaryFolder();
    
    @Test
    public void testDeployedResolver() throws IOException, InterruptedException {
        PicorAssetResolver resolver;
        resolver = new PicorAssetResolver();
        root.newFolder("javascripts");
        root.newFolder("javascripts/reporter");
        root.newFolder("javascripts/reporter/some.js");
        Path actualFile = Files.createFile(FileSystems.getDefault().getPath(root.getRoot().getPath(),"javascripts/reporter/some.js/4212.js"));
        Files.createSymbolicLink(FileSystems.getDefault().getPath(root.getRoot().getPath(), "javascripts/reporter/some.js/some.js"), actualFile);
        resolver.setAssetDirectoryPath(root.getRoot().toString());
        System.err.println(root.getRoot().toString());
        resolver.setCdnPrefix("http://cdn/");
        String result = resolver.resolve(resolver.getMagicPrefix() + "javascripts/reporter/some.js");
        assertEquals("http://cdn/javascripts/reporter/4212/some.js", result);
        result = resolver.resolve(resolver.getMagicPrefix() + "javascripts/reporter/unversioned.js");
        assertEquals("http://cdn/javascripts/reporter/unversioned.js", result);
        
    }
}
