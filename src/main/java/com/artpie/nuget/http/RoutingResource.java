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

import com.artipie.http.Headers;
import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import org.reactivestreams.Publisher;

/**
 * Resource delegating requests handling to other resources, found by routing path.
 *
 * @since 0.1
 */
public final class RoutingResource implements Resource {

    /**
     * Resource path.
     */
    private final String path;

    /**
     * Routes.
     */
    private final Route[] routes;

    /**
     * Ctor.
     *
     * @param path Resource path.
     * @param routes Routes.
     */
    public RoutingResource(final String path, final Route... routes) {
        this.path = path;
        this.routes = Arrays.copyOf(routes, routes.length);
    }

    @Override
    public Response get() {
        return this.resource().get();
    }

    @Override
    public Response put(
        final Headers headers,
        final Publisher<ByteBuffer> body) {
        return this.resource().put(headers, body);
    }

    /**
     * Find resource by path.
     *
     * @return Resource found by path.
     */
    private Resource resource() {
        return Arrays.stream(this.routes)
            .filter(r -> this.path.startsWith(r.path()))
            .max(Comparator.comparing(Route::path))
            .map(r -> r.resource(this.path))
            .orElse(new Absent());
    }

}
