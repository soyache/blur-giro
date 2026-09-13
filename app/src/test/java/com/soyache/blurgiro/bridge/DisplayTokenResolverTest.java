package com.soyache.blurgiro.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisplayTokenResolverTest {
    public static final class Legacy {
        static final Object TOKEN=new Object();
        public static Object getInternalDisplayToken() {return TOKEN;}
    }
    public static final class Modern {
        static final Object TOKEN=new Object();
        public static long[] getPhysicalDisplayIds() {return new long[]{71,72};}
        public static Object getPhysicalDisplayToken(long id) {return id==72?TOKEN:null;}
    }
    public static final class Broken {
        public static Object getInternalDisplayToken() {throw new UnsupportedOperationException("removed");}
    }
    public static final class Empty {
        public static long[] getPhysicalDisplayIds() {return new long[0];}
        public static Object getPhysicalDisplayToken(long id) {return null;}
    }

    @Test public void usesLegacyConvenienceMethodWhenPresent() throws Exception {
        assertSame(Legacy.TOKEN,DisplayTokenResolver.resolve(Legacy.class));
    }
    @Test public void usesPhysicalIdPairWhenConvenienceMethodWasRemoved() throws Exception {
        assertSame(Modern.TOKEN,DisplayTokenResolver.resolve(Modern.class));
    }
    @Test public void continuesAcrossVendorProviderFailures() throws Exception {
        assertSame(Modern.TOKEN,DisplayTokenResolver.resolve(Broken.class,Empty.class,Modern.class));
    }
    @Test public void reportsUnavailableInsteadOfTheMisleadingMissingMethod() {
        try {DisplayTokenResolver.resolve(Broken.class,Empty.class);fail();}
        catch(Exception e) {assertTrue(e.getMessage().contains("token de pantalla física"));}
    }
}
