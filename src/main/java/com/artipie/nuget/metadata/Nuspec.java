/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/nuget-adapter/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

/**
 * Package description in .nuspec format.
 * @since 0.6
 */
public interface Nuspec {

    /**
     * Package identifier: original case sensitive and lowercase.
     * @return Package id
     * @throws ArtipieException If id field is not found
     * @checkstyle MethodNameCheck (3 lines)
     */
    @SuppressWarnings("PMD.ShortMethodName")
    NuspecField id();

    /**
     * Package versions: original version field value or normalised.
     * @return Version of the package
     * @throws ArtipieException If version field is not found
     */
    NuspecField version();

    /**
     * Package description.
     * @return Description
     * @throws ArtipieException If description field is not found
     */
    String description();

    /**
     * Package authors.
     * @return Authors
     * @throws ArtipieException If authors field is not found
     */
    String authors();

    /**
     * Returns optional field by name.
     * @param name Field name
     * @return Optional with value is found
     */
    Optional<String> fieldByName(OptFieldName name);

    /**
     * Nuspec file bytes.
     * @return Bytes
     * @throws ArtipieIOException On OI error
     */
    byte[] bytes();

    /**
     * Implementation of {@link Nuspec}, reads fields values from byte xml source.
     *
     * @since 0.6
     */
    final class Xml implements Nuspec {

        /**
         * Xml document.
         */
        private final XMLDocument content;

        /**
         * Binary content in .nuspec format.
         */
        private final byte[] bytes;

        /**
         * Ctor.
         *
         * @param bytes Binary content of in .nuspec format.
         */
        public Xml(final byte[] bytes) {
            this.bytes = bytes;
            this.content = new XMLDocument(bytes);
        }

        /**
         * Ctor.
         * @param input Input stream with nuspec content
         * @throws ArtipieIOException On IO error
         */
        public Xml(final InputStream input) {
            this(Xml.read(input));
        }

        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public NuspecField id() {
            return new PackageId(
                single(
                    this.content, "/*[name()='package']/*[name()='metadata']/*[name()='id']/text()"
                )
            );
        }

        @Override
        public NuspecField version() {
            final String version = single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='version']/text()"
            );
            return new Version(version);
        }

        @Override
        public String description() {
            return single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='description']/text()"
            );
        }

        @Override
        public String authors() {
            return single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='authors']/text()"
            );
        }

        @Override
        public Optional<String> fieldByName(final OptFieldName name) {
            final List<String> values = this.content.xpath(
                String.format(
                    "/*[name()='package']/*[name()='metadata']/*[name()='%s']/text()", name.get()
                )
            );
            Optional<String> res = Optional.empty();
            if (!values.isEmpty()) {
                res = Optional.of(values.get(0));
            }
            return res;
        }

        @Override
        public byte[] bytes() {
            return this.bytes;
        }

        @Override
        public String toString() {
            return new String(this.bytes(), StandardCharsets.UTF_8);
        }

        /**
         * Reads single string value from XML via XPath.
         * Exception is thrown if zero or more then 1 values found
         *
         * @param xml XML document to read from.
         * @param xpath XPath expression to select data from the XML.
         * @return Value found by XPath
         */
        private static String single(final XML xml, final String xpath) {
            final List<String> values = xml.xpath(xpath);
            if (values.isEmpty()) {
                throw new ArtipieException(
                    new IllegalArgumentException(
                        String.format("No values found in path: '%s'", xpath)
                    )
                );
            }
            if (values.size() > 1) {
                throw new ArtipieException(
                    new IllegalArgumentException(
                        String.format("Multiple values found in path: '%s'", xpath)
                    )
                );
            }
            return values.get(0);
        }

        /**
         * Read bytes from input stream.
         * @param input Input to read from
         * @return Bytes
         */
        private static byte[] read(final InputStream input) {
            try {
                return IOUtils.toByteArray(input);
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }
}
