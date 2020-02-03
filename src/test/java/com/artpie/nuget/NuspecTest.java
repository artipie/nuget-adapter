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

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.google.common.io.ByteSource;
import java.nio.file.Files;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Nuspec}.
 *
 * @since 0.1
 */
class NuspecTest {

    /**
     * Storage used in tests.
     */
    private BlockingStorage storage;

    /**
     * Nuspec created from `newtonsoft.json.nuspec` resource.
     */
    private Nuspec nuspec;

    @BeforeEach
    void init() throws Exception {
        this.storage = new BlockingStorage(
            new FileStorage(
                Files.createTempDirectory(NuspecTest.class.getName()).resolve("repo")
            )
        );
        this.nuspec = new Nuspec(ByteSource.wrap(NewtonJsonPackage.readNuspec()));
    }

    @Test
    void shouldExtractIdentity() {
        final PackageIdentity identity = this.nuspec.identity();
        MatcherAssert.assertThat(
            identity.nuspecKey().string(),
            Matchers.equalTo("newtonsoft.json/12.0.3/newtonsoft.json.nuspec")
        );
    }

    @Test
    void shouldSave() {
        this.nuspec.save(this.storage);
        final Key.From key = new Key.From("newtonsoft.json", "12.0.3", "newtonsoft.json.nuspec");
        MatcherAssert.assertThat(
            this.storage.value(key),
            Matchers.equalTo(NewtonJsonPackage.readNuspec())
        );
    }
}
