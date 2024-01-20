/*
 * Copyright (C) 2018 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.projectparams.processors.files;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;

/**
 * All code in this package is almost straight copy of lombok intercepting file object code
 * if I succeed to comprehend it, I will try to rewrite it in my own way.
 * <p>
 * As I understand for now this code just delegates all calls to our file intercepting object
 */
class FileObjectWrapper implements JavaFileObject {
    private final InterceptingFileObject delegate;

    public FileObjectWrapper(InterceptingFileObject delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return delegate.isNameCompatible(simpleName, kind);
    }

    @Override
    public URI toUri() {
        return delegate.toUri();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return delegate.getCharContent(ignoreEncodingErrors);
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
    public Writer openWriter() throws IOException {
        return delegate.openWriter();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return delegate.openOutputStream();
    }

    @Override
    public long getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public boolean delete() {
        return delegate.delete();
    }

    @Override
    public Kind getKind() {
        return delegate.getKind();
    }

    @Override
    public NestingKind getNestingKind() {
        return delegate.getNestingKind();
    }

    @Override
    public Modifier getAccessLevel() {
        return delegate.getAccessLevel();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileObjectWrapper)) return false;
        return delegate.equals(((FileObjectWrapper) obj).delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}