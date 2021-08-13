package com.joe.activity.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScanUtils {
    public static final String COMPILE_FILE_PATH = "com/cn/joe/apt/stack";
    public static final String MODULE_CLASS = "ActivityStack$";
    public static File FILE_CONTAINS_INIT_CLASS = null;
    public static final String REGISTER_CLASS_FILE_NAME = "cn/soonest/exmoo/common/base/stack/ActivityStackCreator.class";

    static boolean isTargetStackClass(File file) {
        return file.getName().startsWith(MODULE_CLASS);
    }

    static boolean shouldProcessPreDexJar(String path) {
        return !path.contains("com.android.support") && !path.contains("/android/m2repository");
    }

    static List<String> scanJar(File jarFile, File destFile) throws IOException {
        JarFile file = new JarFile(jarFile);
        Enumeration<JarEntry> enumeration = file.entries();
        List<String> list = null;
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            String entryName = jarEntry.getName();

            if (REGISTER_CLASS_FILE_NAME.equals(entryName)) {
                FILE_CONTAINS_INIT_CLASS = destFile;
            } else {
                if (entryName.startsWith(COMPILE_FILE_PATH) && entryName.contains(MODULE_CLASS)) {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    Print.println("module entry name:" + entryName);
                    list.add(entryName.substring(entryName.lastIndexOf("/") + 1));
                }
            }
        }
        return list;
    }
}
