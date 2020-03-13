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

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Label or pre-release part of version.
 * See https://semver.org/spec/v2.0.0.html#spec-item-9.
 *
 * @since 0.1
 */
final class VersionLabel implements Comparable<VersionLabel> {

    /**
     * String representation.
     */
    private final String value;

    /**
     * Ctor.
     *
     * @param value Version label string.
     */
    VersionLabel(final String value) {
        this.value = value;
    }

    @Override
    public int compareTo(final VersionLabel that) {
        final List<Identifier> one = this.identifiers();
        final List<Identifier> two = that.identifiers();
        int compare = 0;
        for (int index = 0; index < one.size(); index += 1) {
            if (index >= two.size()) {
                compare = 1;
                break;
            }
            final int result = one.get(index).compareTo(two.get(index));
            if (result != 0) {
                compare = result;
                break;
            }
        }
        if (compare == 0 && one.size() < two.size()) {
            compare = -1;
        }
        return compare;
    }

    /**
     * Ordered sequence of identifiers representing this label.
     *
     * @return List of identifiers.
     */
    private List<Identifier> identifiers() {
        return Stream.of(this.value.split("\\."))
            .map(Identifier::new)
            .collect(Collectors.toList());
    }

    /**
     * Identifier, part of label.
     *
     * @since 0.1
     */
    private static class Identifier implements Comparable<Identifier> {

        /**
         * String representation.
         */
        private final String value;

        /**
         * Ctor.
         *
         * @param value Version label string.
         */
        Identifier(final String value) {
            this.value = value;
        }

        @Override
        public int compareTo(final Identifier that) {
            final OptionalInt one = this.number();
            final OptionalInt two = that.number();
            final int compare;
            if (one.isPresent()) {
                if (two.isPresent()) {
                    compare = Integer.compare(one.getAsInt(), two.getAsInt());
                } else {
                    compare = -1;
                }
            } else {
                if (two.isPresent()) {
                    compare = 1;
                } else {
                    compare = this.value.compareTo(that.value);
                }
            }
            return compare;
        }

        /**
         * Get numeric representation of identifier.
         *
         * @return Numeric value of identifier, empty if identifier contains some non-digits.
         */
        private OptionalInt number() {
            final OptionalInt res;
            if (this.value.matches("\\d+")) {
                res = OptionalInt.of(Integer.parseInt(this.value));
            } else {
                res = OptionalInt.empty();
            }
            return res;
        }
    }
}
