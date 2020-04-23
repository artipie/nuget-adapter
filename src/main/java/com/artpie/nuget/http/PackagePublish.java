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
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artpie.nuget.InvalidPackageException;
import com.artpie.nuget.PackageVersionAlreadyExistsException;
import com.artpie.nuget.Repository;
import com.artpie.nuget.http.publish.Multipart;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Package publish service, used to pushing new packages and deleting existing ones.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-publish-resource">Push and Delete</a>
 *
 * @since 0.1
 */
public final class PackagePublish implements Route {

    /**
     * Storage to read content from.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage to read content from.
     */
    public PackagePublish(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public String path() {
        return "/package";
    }

    @Override
    public Resource resource(final String path) {
        return new NewPackage(this.storage);
    }

    /**
     * New package resource. Used to push a package into repository.
     * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-publish-resource#push-a-package">Push a package</a>
     *
     * @since 0.1
     */
    public final class NewPackage implements Resource {

        /**
         * Storage to read content from.
         */
        private final Storage storage;

        /**
         * Ctor.
         *
         * @param storage Storage to read content from.
         */
        public NewPackage(final Storage storage) {
            this.storage = storage;
        }

        @Override
        public Response get() {
            return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }

        @Override
        public Response put(
            final Headers headers,
            final Publisher<ByteBuffer> body
        ) {
            return new AsyncResponse(
                CompletableFuture
                    .supplyAsync(() -> new Key.From(UUID.randomUUID().toString()))
                    .thenCompose(
                        key -> this.storage.save(key, new Multipart(headers, body).first())
                            .thenApply(
                                ignored -> {
                                    RsStatus status;
                                    try {
                                        new Repository(
                                            new BlockingStorage(this.storage)
                                        ).add(key);
                                        status = RsStatus.CREATED;
                                    } catch (final IOException ex) {
                                        throw new IllegalStateException(ex);
                                    } catch (final InvalidPackageException ex) {
                                        status = RsStatus.BAD_REQUEST;
                                    } catch (final PackageVersionAlreadyExistsException ex) {
                                        status = RsStatus.CONFLICT;
                                    }
                                    return new RsWithStatus(status);
                                }
                            )
                    )
            );
        }
    }
}
