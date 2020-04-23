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
package com.artpie.nuget.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.google.common.io.Resources;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NuGet}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
class NuGetTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        this.nuget = new NuGet(new URL("http://localhost"), "/base", this.storage);
    }

    @Test
    void shouldGetPackageContent() {
        final byte[] data = "data".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package", "1.0.0", "content.nupkg"),
            data
        );
        MatcherAssert.assertThat(
            "Package content should be returned in response",
            this.nuget.response(
                "GET /base/content/package/1.0.0/content.nupkg",
                Collections.emptyList(),
                Flowable.empty()
            ),
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                )
            )
        );
    }

    @Test
    void shouldFailGetPackageContentFromNotBasePath() {
        final Response response = this.nuget.response(
            "GET /not-base/content/package/1.0.0/content.nupkg",
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Resources from outside of base path should not be found",
            response,
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldFailGetPackageContentWhenNotExists() {
        MatcherAssert.assertThat(
            "Not existing content should not be found",
            this.nuget.response(
                "GET /base/content/package/1.0.0/logo.png",
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldFailPutPackageContent() {
        final Response response = this.nuget.response(
            "PUT /base/content/package/1.0.0/content.nupkg",
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Package content cannot be put",
            response,
            new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED)
        );
    }

    @Test
    void shouldPutPackagePublish() throws Exception {
        final Response response = this.putPackage(nupkg());
        MatcherAssert.assertThat(
            response,
            new RsHasStatus(RsStatus.CREATED)
        );
    }

    @Test
    void shouldFailPutPackage() throws Exception {
        MatcherAssert.assertThat(
            "Should fail to add package which is not a ZIP archive",
            this.putPackage("not a zip".getBytes()),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
    }

    @Test
    void shouldFailPutSamePackage() throws Exception {
        this.putPackage(nupkg()).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Should fail to add same package when it is already present in the repository",
            this.putPackage(nupkg()),
            new RsHasStatus(RsStatus.CONFLICT)
        );
    }

    @Test
    void shouldFailGetPackagePublishFromNotBasePath() {
        final Response response = this.nuget.response(
            "GET /not-base/package",
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.NOT_FOUND));
    }

    @Test
    void shouldFailGetPackagePublish() {
        final Response response = this.nuget.response(
            "GET /base/package",
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED));
    }

    @Test
    void shouldGetPackageVersions() {
        final byte[] data = "example".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package2", "index.json"),
            data
        );
        MatcherAssert.assertThat(
            this.nuget.response(
                "GET /base/content/package2/index.json",
                Collections.emptyList(),
                Flowable.empty()
            ),
            Matchers.allOf(
                new RsHasStatus(RsStatus.OK),
                new RsHasBody(data)
            )
        );
    }

    @Test
    void shouldFailGetPackageVersionsWhenNotExists() {
        MatcherAssert.assertThat(
            this.nuget.response(
                "GET /base/content/unknown-package/index.json",
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    private Response putPackage(final byte[] pack) throws Exception {
        final HttpEntity entity = MultipartEntityBuilder.create()
            .addBinaryBody("package.nupkg", pack)
            .build();
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        entity.writeTo(sink);
        return this.nuget.response(
            "PUT /base/package",
            new Headers.From(
                "Content-Type", entity.getContentType().getValue()
            ),
            Flowable.fromArray(ByteBuffer.wrap(sink.toByteArray()))
        );
    }

    private static byte[] nupkg() throws Exception {
        final URL resource = Thread.currentThread().getContextClassLoader()
            .getResource("newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg");
        return Resources.toByteArray(resource);
    }
}
