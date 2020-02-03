/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artpie.nuget;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;

/**
 * Newton.Json package resources.
 *
 * @since 0.1
 */
final class NewtonJsonPackage {

    /**
     * Ctor.
     */
    private NewtonJsonPackage() {
    }

    /**
     * Read .nuspec file content.
     *
     * @return Binary data.
     */
    static byte[] readNuspec() {
        return read("newtonsoft.json.nuspec");
    }

    /**
     * Reads file content.
     *
     * @param name File name.
     * @return Binary data.
     */
    private static byte[] read(final String name) {
        final String resource = String.format("newtonsoft.json/12.0.3/%s", name);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            if (stream == null) {
                final String message = String.format("Cannot find resource by name '%s'", name);
                throw new IllegalArgumentException(message);
            }
            return ByteStreams.toByteArray(stream);
        } catch (final IOException ex) {
            throw new IllegalArgumentException(String.format("Failed to read '%s'", name), ex);
        }
    }
}
