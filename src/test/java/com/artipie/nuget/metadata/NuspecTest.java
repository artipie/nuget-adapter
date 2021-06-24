/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/nuget-adapter/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.nuget.NewtonJsonResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Nuspec.Xml}.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NuspecTest {

    @Test
    void returnsBytes() {
        final byte[] nuspec = new NewtonJsonResource("newtonsoft.json.nuspec").bytes();
        MatcherAssert.assertThat(
            new Nuspec.Xml(nuspec).bytes(),
            new IsEqual<>(nuspec)
        );
    }

    @Test
    void readsVersion() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .version().raw(),
            new IsEqual<>("12.0.3")
        );
    }

    @Test
    void readsId() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .id().raw(),
            new IsEqual<>("Newtonsoft.Json")
        );
    }

    @Test
    void readsAuthors() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .authors(),
            new IsEqual<>("James Newton-King")
        );
    }

    @Test
    void readsDescription() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .description(),
            new IsEqual<>("Json.NET is a popular high-performance JSON framework for .NET")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "TITLE,Json.NET",
        "LICENSE_URL,https://licenses.nuget.org/MIT",
        "REQUIRE_LICENSE_ACCEPTANCE,false",
        "TAGS,json",
        "PROJECT_URL,https://www.newtonsoft.com/json"
    })
    void readsOptField(final OptFieldName name, final String val) {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .fieldByName(name).get(),
            new IsEqual<>(val)
        );
    }

    @Test
    void returnsEmptyWhenFieldIsAbsent() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .fieldByName(OptFieldName.RELEASE_NOTES).isPresent(),
            new IsEqual<>(false)
        );
    }

}
