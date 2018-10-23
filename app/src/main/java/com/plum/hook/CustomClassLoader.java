package com.plum.hook;

import dalvik.system.DexClassLoader;

/**
 * Created by mei on 2018/10/23.
 * Description:
 */
public class CustomClassLoader extends DexClassLoader {
    public CustomClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
