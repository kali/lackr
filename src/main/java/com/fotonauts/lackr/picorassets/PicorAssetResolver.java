package com.fotonauts.lackr.picorassets;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class PicorAssetResolver implements AssetResolver {

    private String magicPrefix = "/lackr.prefix.for.assets/";
    private String assetDirectoryPath;
    private String cdnPrefix;

    // picor uses <magicPrefix>/javascript/reporter/crap.js
    // <assetDirectoryPath>/javascript/reporter/crap.js/crap.js is expected to be a symlink to
    // <assetDirectoryPath>/javascript/reporter/crap.js/$$sha1.js
    // lackr inserts: <cdnPrefix>/javascript/reporter/$$sha1/crap.js
    // nginx receives /javascript/reporter/$$sha1/crap.js
    @Override
    public String resolve(String asset) {
        String strippedAsset = asset.substring(magicPrefix.length());
        System.err.println("resolving " + asset + " . stripped:" + strippedAsset);
        Path versionsDirectory = FileSystems.getDefault().getPath(assetDirectoryPath, strippedAsset);
        System.err.println("resolving " + asset + " . versions:" + versionsDirectory.toString());
        if (versionsDirectory.toFile().isDirectory()) {
            System.err.println("resolving " + asset + " . is a directory.");
            Path link = versionsDirectory.resolve(versionsDirectory.getFileName());
            System.err.println("resolving " + asset + " . link: " + link.toString());
            try {
                Path resolved = link.toRealPath();
                System.err.println("resolving " + asset + " . to: " + resolved.toString());
                String sha1 = resolved.getFileName().toString().substring(0, resolved.getFileName().toString().lastIndexOf('.'));
                System.err.println("resolving " + asset + " . sha1: " + sha1);
                File strippedAssetAsFile = new File(strippedAsset);
                return cdnPrefix + strippedAssetAsFile.getParent() + "/" + sha1 + "/" + strippedAssetAsFile.getName();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return cdnPrefix + strippedAsset;            
        }
    }

    @Override
    public String getMagicPrefix() {
        return magicPrefix;
    }

    public void setMagicPrefix(String magicPrefix) {
        this.magicPrefix = magicPrefix;
    }

    public String getAssetDirectoryPath() {
        return assetDirectoryPath;
    }

    public void setAssetDirectoryPath(String assetDirectoryPath) {
        this.assetDirectoryPath = assetDirectoryPath;
    }

    public String getCdnPrefix() {
        return cdnPrefix;
    }

    public void setCdnPrefix(String cdnPrefix) {
        this.cdnPrefix = cdnPrefix;
    }
}
