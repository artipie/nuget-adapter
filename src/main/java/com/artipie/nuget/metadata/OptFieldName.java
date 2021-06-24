/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/nuget-adapter/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

/**
 * Names of the optional fields of nuspes file.
 * @since 0.7
 * @checkstyle JavadocVariableCheck (500 lines)
 */
public enum OptFieldName {

    TITLE("title"),

    LICENSE_URL("licenseUrl"),

    REQUIRE_LICENSE_ACCEPTANCE("requireLicenseAcceptance"),

    TAGS("tags"),

    PROJECT_URL("projectUrl"),

    RELEASE_NOTES("releaseNotes");

    /**
     * Xml field name.
     */
    private final String name;

    /**
     * Ctor.
     * @param name Xml field name
     */
    OptFieldName(final String name) {
        this.name = name;
    }

    /**
     * Get xml field name.
     * @return String xml name
     */
    public String get() {
        return this.name;
    }
}
