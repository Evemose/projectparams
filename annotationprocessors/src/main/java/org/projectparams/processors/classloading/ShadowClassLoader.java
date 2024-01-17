package org.projectparams.processors.classloading;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

class ShadowClassLoader extends ClassLoader {
    private final String SELF_BASE;
    private final ClassLoader parent;
    private final String SCL_SUFFIX = "projectparams";
    private final String PATH_TO_SELF = "org/projectparams/processors/classloading/ShadowClassLoader.class";
    private final File SELF_BASE_FILE;

    ShadowClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;

        var sclClassUrl = ShadowClassLoader.class.getResource("ShadowClassLoader.class");
        var sclClassStr = Objects.requireNonNull(sclClassUrl).toString();
        SELF_BASE = urlDecode(sclClassStr.substring(0, sclClassStr.length() - PATH_TO_SELF.length()));
        SELF_BASE_FILE = getSelfBaseFile();

        var scl = System.getProperty("shadow.override." + SCL_SUFFIX);
        if (scl != null && !scl.isEmpty()) {
            for (var part : scl.split("\\s*" + File.pathSeparatorChar + "\\s*")) {
//                if (part.endsWith("/*") || part.endsWith(File.separator + "*")) {
//                    addOverrideJarDir(part.substring(0, part.length() - 2));
//                } else {
//                    addOverrideClasspathEntry(part);
//                }
            }
        }
    }

    private File getSelfBaseFile() {
        final File SELF_BASE_FILE;
        if (SELF_BASE.startsWith("jar:file:") && SELF_BASE.endsWith("!/")) SELF_BASE_FILE = new File(SELF_BASE.substring(9, SELF_BASE.length() - 2));
        else if (SELF_BASE.startsWith("file:")) SELF_BASE_FILE = new File(SELF_BASE.substring(5));
        else SELF_BASE_FILE = new File(SELF_BASE);
        return SELF_BASE_FILE;
    }

    private String toSclResourceName(String name) {
        return "SCL." + SCL_SUFFIX + "/" + name.substring(0, name.length() - 6) + ".SCL." + SCL_SUFFIX;
    }

//    @Override
//    public Enumeration<URL> getResources(String name) throws IOException {
//        String shadowName = null;
//        if (name.endsWith(".class")) shadowName = toSclResourceName(name);
//
//        var resourceUrls = new ArrayList<URL>();
//
//        for (File ce : override) {
//            var url = getResourceFromLocation(name, shadowName, ce);
//            if (url != null) resourceUrls.add(url);
//        }
//
//        if (override.isEmpty()) {
//            var fromSelf = getResourceFromLocation(name, shadowName, SELF_BASE_FILE);
//            if (fromSelf != null) resourceUrls.add(fromSelf);
//        }
//
//        var sec = super.getResources(name);
//        while (sec.hasMoreElements()) {
//            var item = sec.nextElement();
//            if (isPartOfShadowSuffix(item.toString(), name, sclSuffix)) resourceUrls.add(item);
//        }
//
//        if (shadowName != null) {
//            var tern = super.getResources(shadowName);
//            while (tern.hasMoreElements()) {
//                var item = tern.nextElement();
//                if (isPartOfShadowSuffix(item.toString(), shadowName, sclSuffix)) resourceUrls.add(item);
//            }
//        }
//
//        return Collections.enumeration(resourceUrls);
//    }

//    @Override
//    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        Class<?> alreadyLoaded = findLoadedClass(name);
//        if (alreadyLoaded != null) return alreadyLoaded;
//
////        not sure if this is needed
////        if (highlanders.contains(name)) {
////            Class<?> c = highlanderMap.get(name);
////            if (c != null) return c;
////        }
//
//        var classFileName = NameUtils.toClassFileName(name);
//        URL res = getResourceInternal(classFileName, true);
//        if (res == null) {
//            if (!exclusionListMatch(classFileName)) {
//                try {
//                    // First search in the prepended classloaders, the class might be there already
//                    for (ClassLoader pre : prependedParentLoaders) {
//                        try {
//                            Class<?> loadClass = pre.loadClass(name);
//                            if (loadClass != null) return loadClass;
//                        } catch (Throwable ignored) {
//                        }
//                    }
//                    return super.loadClass(name, resolve);
//                } catch (ClassNotFoundException e) {
//                    res = getResourceInternal("secondaryLoading" + toSclResourceName(name), true);
//                    if (res == null) throw e;
//                }
//            }
//        }
//        if (res == null) throw new ClassNotFoundException(name);
//
//        return urlToDefineClass(name, res, resolve);
//    }

    private static String urlDecode(String str) {
        return URLDecoder.decode(str.replaceAll("\\+", "%2B"), StandardCharsets.UTF_8);
    }
}
