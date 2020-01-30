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

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Package data binary stream in .nupkg format.
 *
 * @since 0.1
 */
public final class PackageBinaryStream {

    /**
     * Binary stream containing package data.
     */
    private final InputStream stream;

    /**
     * Ctor.
     *
     * @param stream Package data.
     */
    public PackageBinaryStream(final InputStream stream) {
        this.stream = stream;
    }

    /**
     * Parses meta information from package content.
     *
     * @return Package metadata parsed from the package.
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public PackageMeta readMeta() {
        try (ZipInputStream zipStream = new ZipInputStream(this.stream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".nuspec")) {
                    final PackageId id = parseNuspecXml(zipStream);
                    return new PackageMeta(id);
                }
            }
            throw new IllegalArgumentException("No .nuspec file found inside the package.");
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed reading meta from stream", ex);
        }
    }

    /**
     * Parses .nuspec XML from the stream.
     *
     * @param stream Stream to read XML from.
     * @return Info parsed from the XML.
     */
    private static PackageId parseNuspecXml(final InputStream stream) {
        final Document doc;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(stream);
        } catch (final IOException | ParserConfigurationException | SAXException ex) {
            throw new IllegalArgumentException("Failed parsing .nuspec XML", ex);
        }
        final XML xml = new XMLDocument(doc);
        final String id = single(xml, "/package/metadata/id/text()");
        final String version = single(xml, "/package/metadata/version/text()");
        return new PackageId(id, version);
    }

    /**
     * Reads single string value from XML by XPath.
     * Exception is thrown if zero or more then 1 values found
     *
     * @param xml XML document to read from.
     * @param xpath XPath expression to select data from the XML.
     * @return Value found by XPath
     */
    private static String single(final XML xml, final String xpath) {
        final List<String> values = xml.xpath(xpath);
        if (values.isEmpty()) {
            final String message = String.format("No values found in path: '%s'", xpath);
            throw new IllegalArgumentException(message);
        }
        if (values.size() > 1) {
            final String message = String.format("Multiple values found in path: '%s'", xpath);
            throw new IllegalArgumentException(message);
        }
        return values.get(0);
    }
}
