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
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artpie.nuget.PackageIdentity;
import com.artpie.nuget.http.metadata.ContentLocation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Package content route.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-base-address-resource">Package Content</a>
 *
 * @since 0.1
 */
public final class PackageContent implements Route, ContentLocation {

    /**
     * Base URL of repository.
     */
    private final URL base;

    /**
     * Storage to read content from.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param base Base URL of repository.
     * @param storage Storage to read content from.
     */
    public PackageContent(final URL base, final Storage storage) {
        this.base = base;
        this.storage = storage;
    }

    @Override
    public String path() {
        return "/content";
    }

    @Override
    public Resource resource(final String path) {
        return new PackageResource(path, this.storage);
    }

    @Override
    public URL url(final PackageIdentity identity) {
        final String relative = String.format(
            "%s%s/%s",
            this.base.getPath(),
            this.path(),
            identity.nupkgKey().string()
        );
        try {
            return new URL(this.base, relative);
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException(
                String.format("Failed to build URL from base: '%s'", this.base),
                ex
            );
        }
    }

    /**
     * Package content resource.
     *
     * @since 0.1
     */
    private class PackageResource implements Resource {

        /**
         * Resource path.
         */
        private final String path;

        /**
         * Storage to read content from.
         */
        private final Storage storage;

        /**
         * Ctor.
         *
         * @param path Resource path.
         * @param storage Storage to read content from.
         */
        PackageResource(final String path, final Storage storage) {
            this.path = path;
            this.storage = storage;
        }

        @Override
        public Response get() {
            return new AsyncResponse(
                this.existing().thenCompose(
                    existing -> existing.<CompletionStage<Response>>map(
                        key -> this.storage.value(key).thenApply(
                            data -> new RsWithBody(new RsWithStatus(RsStatus.OK), data)
                        )
                    ).orElseGet(
                        () -> CompletableFuture.completedFuture(
                            new RsWithStatus(RsStatus.NOT_FOUND)
                        )
                    )
                )
            );
        }

        @Override
        public Response put(
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }

        /**
         * Try build key from path and check if it exists.
         *
         * @return Key to storage value, if value exists.
         */
        private CompletableFuture<Optional<Key>> existing() {
            return CompletableFuture.supplyAsync(this::key)
                .thenCompose(
                    parsed -> {
                        final CompletableFuture<Optional<Key>> found;
                        if (parsed.isPresent()) {
                            final Key key = parsed.get();
                            found = this.storage.exists(key).thenApply(
                                exists -> {
                                    final Optional<Key> existing;
                                    if (exists) {
                                        existing = Optional.of(key);
                                    } else {
                                        existing = Optional.empty();
                                    }
                                    return existing;
                                }
                            );
                        } else {
                            found = CompletableFuture.completedFuture(Optional.empty());
                        }
                        return found;
                    }
                );
        }

        /**
         * Tries to build key to storage value from path.
         *
         * @return Key to storage value, if there is one.
         */
        private Optional<Key> key() {
            final String prefix = String.format("%s/", path());
            final Optional<Key> parsed;
            if (this.path.startsWith(prefix)) {
                parsed = Optional.of(new Key.From(this.path.substring(prefix.length())));
            } else {
                parsed = Optional.empty();
            }
            return parsed;
        }
    }
}
