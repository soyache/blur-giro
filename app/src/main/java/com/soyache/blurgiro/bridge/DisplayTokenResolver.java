/*
 * Adapted from DuoFold-Android (https://github.com/jcx396905-gif/DuoFold-Android)
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: same AOSP / vendor display-token fallbacks.
 */
package com.soyache.blurgiro.bridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Resolves the default physical display across AOSP and vendor hidden-API layouts. */
final class DisplayTokenResolver {
    private DisplayTokenResolver() {}

    static Object resolve(Class<?>... providers) throws Exception {
        Throwable last=null;
        for(Class<?> provider:providers) {
            if(provider==null)continue;
            try {
                Method direct=provider.getMethod("getInternalDisplayToken");
                Object token=direct.invoke(null);
                if(token!=null)return token;
            } catch(NoSuchMethodException e) {last=e;}
            catch(InvocationTargetException e) {last=e.getCause()==null?e:e.getCause();}
            try {
                long[] ids=(long[])provider.getMethod("getPhysicalDisplayIds").invoke(null);
                if(ids!=null)for(long id:ids) {
                    Object token=provider.getMethod("getPhysicalDisplayToken",long.class).invoke(null,id);
                    if(token!=null)return token;
                }
            } catch(NoSuchMethodException e) {last=e;}
            catch(InvocationTargetException e) {last=e.getCause()==null?e:e.getCause();}
        }
        throw new IllegalStateException("No hay API de token de pantalla física",last);
    }
}
