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
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link SliceFromResource}.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class SliceFromResourceTest {

    /**
     * HTTP version string.
     */
    private static final String HTTP_VERSION = "HTTP/1.1";

    @Test
    void shouldDelegateGetResponse() {
        final RsStatus status = RsStatus.OK;
        final Header header = new Header("Name", "Value");
        final byte[] body = "body".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(final Headers headers) {
                    return new RsFull(
                        status,
                        headers,
                        Flowable.just(ByteBuffer.wrap(body))
                    );
                }

                @Override
                public Response put(final Headers headers, final Publisher<ByteBuffer> body) {
                    throw new UnsupportedOperationException();
                }
            }
        ).response(
            new RequestLine(
                RqMethod.GET.value(),
                "/some/path",
                SliceFromResourceTest.HTTP_VERSION
            ).toString(),
            new Headers.From(Collections.singleton(header)),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(body)
            )
        );
    }

    @Test
    void shouldDelegatePutResponse() {
        final RsStatus status = RsStatus.OK;
        final Header header = new Header("X-Name", "Something");
        final byte[] content = "content".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(final Headers headers) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Response put(final Headers headers, final Publisher<ByteBuffer> body) {
                    return new RsFull(status, headers, body);
                }
            }
        ).response(
            new RequestLine(
                RqMethod.PUT.value(),
                "/some/other/path",
                SliceFromResourceTest.HTTP_VERSION
            ).toString(),
            new Headers.From(Collections.singleton(header)),
            Flowable.just(ByteBuffer.wrap(content))
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(content)
            )
        );
    }
}
