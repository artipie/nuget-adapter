/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/nuget-adapter/artipie/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.nuget.Repository;
import com.artipie.nuget.http.content.PackageContent;
import com.artipie.nuget.http.index.ServiceIndex;
import com.artipie.nuget.http.metadata.PackageMetadata;
import com.artipie.nuget.http.publish.PackagePublish;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * NuGet repository HTTP front end.
 *
 * @since 0.1
 * @todo #84:30min Refactor NuGet class, reduce number of fields.
 *  There are too many fields and constructor parameters as result in this class.
 *  Probably it is needed to extract some additional abstractions to reduce it,
 *  joint Permissions and Identities might be one of them.
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
public final class NuGet implements Slice {

    /**
     * Base URL.
     */
    private final URL url;

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Access permissions.
     */
    private final Permissions perms;

    /**
     * User identities.
     */
    private final Authentication users;

    /**
     * Ctor.
     *
     * @param url Base URL.
     * @param repository Repository.
     */
    public NuGet(final URL url, final Repository repository) {
        this(url, repository, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor.
     *
     * @param url Base URL.
     * @param repository Storage for packages.
     * @param perms Access permissions.
     * @param users User identities.
     */
    public NuGet(
        final URL url,
        final Repository repository,
        final Permissions perms,
        final Authentication users
    ) {
        this.url = url;
        this.repository = repository;
        this.perms = perms;
        this.users = users;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        final Resource resource = this.resource(path);
        final RqMethod method = request.method();
        if (method.equals(RqMethod.GET)) {
            response = resource.get(new Headers.From(headers));
        } else if (method.equals(RqMethod.PUT)) {
            response = resource.put(new Headers.From(headers), body);
        } else {
            response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
        return response;
    }

    /**
     * Find resource by relative path.
     *
     * @param path Relative path.
     * @return Resource found by path.
     */
    private Resource resource(final String path) {
        final PackagePublish publish = new PackagePublish(this.repository);
        final PackageContent content = new PackageContent(this.url, this.repository);
        final PackageMetadata metadata = new PackageMetadata(this.repository, content);
        return new RoutingResource(
            path,
            new ServiceIndex(
                Arrays.asList(
                    new RouteService(this.url, publish, "PackagePublish/2.0.0"),
                    new RouteService(this.url, metadata, "RegistrationsBaseUrl/Versioned"),
                    new RouteService(this.url, content, "PackageBaseAddress/3.0.0")
                )
            ),
            this.auth(publish, Action.Standard.WRITE),
            this.auth(content, Action.Standard.READ),
            this.auth(metadata, Action.Standard.READ)
        );
    }

    /**
     * Create route supporting basic authentication.
     *
     * @param route Route requiring authentication.
     * @param action Action.
     * @return Authenticated route.
     */
    private Route auth(final Route route, final Action action) {
        return new BasicAuthRoute(
            route,
            new Permission.ByName(this.perms, action),
            this.users
        );
    }
}
