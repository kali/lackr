package com.fotonauts.lackr.picorassets;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.PrefixDetectingRule;

public class AssetPrefixRule extends PrefixDetectingRule {
    
    private AssetResolver resolver;

    public AssetPrefixRule() {
    }

    public AssetPrefixRule(AssetResolver assetResolver) {
        setResolver(assetResolver);    
    }

    @Override
    public Chunk substitute(byte[] buffer, int start, int stop, Object context) {
        try {
            return new ConstantChunk(resolver.resolve(new String(buffer, start, stop - start, "UTF-8")).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null; // na
        }
    }

    @Override
    public int lookaheadForEnd(byte[] buffer, int start, int stop) {
        int lookahead = start + trigger.length();
        boolean goOn = true;
        while (goOn && lookahead < stop) {
            byte c = buffer[lookahead];
            goOn = (c >= '-' && c <= '9') // - . / 0-9
                    || (c >= '@' && c <= 'Z') // @ A-Z
                    || (c >= 'a' && c <= 'z') // a-z
                    || (c == '_') || (c == '+'); 
            if(goOn)
                lookahead+=1;
        }
        return lookahead;
    }

    public AssetResolver getResolver() {
        return resolver;
    }

    public void setResolver(AssetResolver resolver) {
        this.resolver = resolver;
        setTrigger(resolver.getMagicPrefix());
    }

}
