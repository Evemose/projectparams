package org.projectparams.processors.files;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Most of the code in this package is almost straight copy of lombok intercepting file object code
 * if I succeed to comprehend it, I will try to rewrite it in my own way
 */
public class InterceptingFileObject implements JavaFileObject {
    private final JavaFileObject delegate;
    private final String fileName;

    public InterceptingFileObject(JavaFileObject original, String fileName) {
        this.delegate = original;
        this.fileName = fileName;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return delegate.openOutputStream();
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException("Can't use a write for class files");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InterceptingFileObject iObj) {
            if (obj == this) {
                return true;
            }
            return fileName.equals(iObj.fileName) && delegate.equals(iObj.delegate);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fileName.hashCode() ^ delegate.hashCode();
    }

    @Override
    public boolean delete() {
        return delegate.delete();
    }

    @Override
    public Modifier getAccessLevel() {
        return delegate.getAccessLevel();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return delegate.getCharContent(ignoreEncodingErrors);
    }

    @Override
    public Kind getKind() {
        return delegate.getKind();
    }

    @Override
    public long getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    @SuppressWarnings("all")
    public String getName() {
        return delegate.getName();
    }

    @Override
    public NestingKind getNestingKind() {
        return delegate.getNestingKind();
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return delegate.isNameCompatible(simpleName, kind);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return delegate.openInputStream();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return delegate.openReader(ignoreEncodingErrors);
    }

    @Override
    public URI toUri() {
        return delegate.toUri();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
