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

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artpie.nuget.Repository;
import com.artpie.nuget.http.metadata.PackageMetadata;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * NuGet repository HTTP front end.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
public final class NuGet implements Slice {

    /**
     * Base URL.
     */
    private final URL url;

    /**
     * Base path.
     */
    private final String base;

    /**
     * Storage for packages.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param url Base URL.
     * @param base Base path.
     * @param storage Storage for packages.
     */
    public NuGet(final URL url, final String base, final Storage storage) {
        this.url = url;
        this.base = base;
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response response;
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        if (path.startsWith(this.base)) {
            final Resource resource = this.resource(path.substring(this.base.length()));
            final RqMethod method = request.method();
            if (method.equals(RqMethod.GET)) {
                response = resource.get();
            } else if (method.equals(RqMethod.PUT)) {
                response = resource.put(new Headers.From(headers), body);
            } else {
                response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
            }
        } else {
            response = new RsWithStatus(RsStatus.NOT_FOUND);
        }
        return response;
    }

    /**
     * Find resource by relative path.
     *
     * @param path Relative path.
     * @return Resource found by path.
     */
    private Resource resource(final String path) {
        final PackagePublish publish = new PackagePublish(this.storage);
        final PackageContent content = new PackageContent(this.url, this.storage);
        final PackageMetadata metadata = new PackageMetadata(
            new Repository(new BlockingStorage(this.storage)),
            content
        );
        return new RoutingResource(
            path,
            new ServiceIndex(
                Arrays.asList(
                    new RouteService(this.url, publish, "PackagePublish/2.0.0"),
                    new RouteService(this.url, metadata, "RegistrationsBaseUrl/Versioned"),
                    new RouteService(this.url, content, "PackageBaseAddress/3.0.0")
                )
            ),
            publish,
            content,
            metadata
        );
    }
}
