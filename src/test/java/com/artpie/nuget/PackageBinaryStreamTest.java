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

import java.io.InputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PackageBinaryStream}.
 *
 * @since 0.1
 */
public class PackageBinaryStreamTest {

    @Test
    void shouldReadMeta() throws Exception {
        final PackageMeta meta;
        try (InputStream stream = getNewtonJsonPackageAsStream()) {
            meta = new PackageBinaryStream(stream).readMeta();
        }
        MatcherAssert.assertThat(meta, Matchers.notNullValue());
        MatcherAssert.assertThat(meta.getId(), Matchers.notNullValue());
        final PackageId id = meta.getId();
        MatcherAssert.assertThat(id.getId(), Matchers.is("Newtonsoft.Json"));
        MatcherAssert.assertThat(id.getVersion(), Matchers.is("12.0.3"));
    }

    private static InputStream getNewtonJsonPackageAsStream() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResourceAsStream("newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg");
    }
}
