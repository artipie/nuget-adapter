/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/nuget-adapter/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

/**
 * Nuspec xml metadata field.
 * @since 0.6
 */
public interface NuspecField {

    /**
     * Original row value (as it was in xml).
     * @return String value
     */
    String row();

    /**
     * Normalized value of the field.
     * @return Normalized value
     */
    String normalized();

}