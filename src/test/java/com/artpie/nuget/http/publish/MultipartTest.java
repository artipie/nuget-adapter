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
package com.artpie.nuget.http.publish;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import com.artipie.http.Headers;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Multipart}.
 *
 * @since 0.1
 */
class MultipartTest {

    @Test
    void shouldReadFirstPart() {
        final Multipart multipart = new Multipart(
            new Headers.From("Content-Type", "multipart/form-data; boundary=\"simple boundary\""),
            Flowable.just(
                ByteBuffer.wrap(
                    String.join(
                        "",
                        "--simple boundary\r\n",
                        "Some-Header: info\r\n",
                        "\r\n",
                        "data\r\n",
                        "--simple boundary--"
                    ).getBytes()
                )
            )
        );
        MatcherAssert.assertThat(
            new Remaining(new Concatenation(multipart.first()).single().blockingGet()).bytes(),
            new IsEqual<>("data".getBytes())
        );
    }

    @Test
    void shouldFailIfNoContentTypeHeader() {
        final Multipart multipart = new Multipart(Collections.emptySet(), Flowable.empty());
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> Flowable.fromPublisher(multipart.first()).blockingFirst()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new IsEqual<>("Cannot find header \"Content-Type\"")
        );
    }

    @Test
    void shouldFailIfNoParts() {
        final Multipart multipart = new Multipart(
            new Headers.From("content-type", "multipart/form-data; boundary=123"),
            Flowable.just(ByteBuffer.wrap("--123--".getBytes()))
        );
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> Flowable.fromPublisher(multipart.first()).blockingFirst()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new IsEqual<>("Body has no parts")
        );
    }
}
