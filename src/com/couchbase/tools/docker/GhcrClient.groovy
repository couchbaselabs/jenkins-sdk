package com.couchbase.tools.docker

import groovy.json.JsonSlurper

/**
 * Reusable client for querying container versions of a GHCR (GitHub Container
 * Registry) package, given its github.com package URL, e.g.
 * https://github.com/couchbaselabs/transactions-fit-performer/pkgs/container/java-fit-performer
 *
 * Requires a GitHub token with the read:packages scope.
 */
class GhcrClient {

    static class Target {
        String owner
        String pkg
    }

    /** Thrown when the package cannot be found under either an org or a user. */
    static class PackageNotFoundException extends RuntimeException {
        PackageNotFoundException(String message) { super(message) }
    }

    /** Thrown when GitHub rejects the request (auth, missing scope, or any other HTTP error). */
    static class GhcrHttpException extends RuntimeException {
        final int code
        GhcrHttpException(int code, String message) {
            super(message)
            this.code = code
        }
    }

    private static class NotFound extends RuntimeException {}

    /** Pure parser: extracts owner and package name from a GHCR package URL. */
    static Target parseUrl(String url) {
        def m = (url =~ /github\.com\/([^\/]+)\/[^\/]+\/pkgs\/container\/([^\/?#]+)/)
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "URL does not match expected shape " +
                    "https://github.com/{owner}/{repo}/pkgs/container/{package} : ${url}")
        }
        def t = new Target()
        t.owner = m.group(1)
        t.pkg = m.group(2)
        return t
    }

    /** Parse a GHCR package URL and fetch its versions in one call. */
    static List fetchVersions(String url, String token, Integer limit = null) {
        return fetchVersions(parseUrl(url), token, limit)
    }

    /**
     * Returns every container version (most recent first, as GitHub orders them),
     * with all available metadata, for the given package.
     *
     * @param limit if non-null, stop after this many versions.
     * @throws PackageNotFoundException if the package is not found under org or user.
     * @throws GhcrHttpException        if GitHub rejects the request.
     */
    static List fetchVersions(Target target, String token, Integer limit = null) {
        for (String kind in ["orgs", "users"]) {
            def all = []
            String url = "https://api.github.com/${kind}/${target.owner}/packages/container/${target.pkg}/versions?per_page=100"
            try {
                while (url != null) {
                    HttpURLConnection conn = open(url, token)
                    int code = conn.responseCode

                    if (code == 404) {
                        throw new NotFound()
                    }
                    if (code == 401 || code == 403) {
                        throw new GhcrHttpException(code,
                                "HTTP ${code}: token rejected or missing read:packages scope.")
                    }
                    if (code >= 400) {
                        throw new GhcrHttpException(code,
                                "HTTP ${code} fetching ${url}: ${conn.errorStream?.text}")
                    }

                    def page = new JsonSlurper().parseText(conn.inputStream.getText())
                    for (v in page) {
                        all.add(v)
                        if (limit != null && all.size() >= limit) {
                            return all
                        }
                    }
                    url = nextLink(conn)
                }
                return all
            }
            catch (NotFound ignored) {
                // Owner is not an org of this kind; fall through and try the next kind.
            }
        }

        throw new PackageNotFoundException(
                "Package not found: ${target.owner}/${target.pkg} (tried both organization and user).")
    }

    private static HttpURLConnection open(String url, String token) {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
        conn.setRequestProperty("Authorization", "Bearer ${token}")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return conn
    }

    private static String nextLink(HttpURLConnection conn) {
        def link = conn.getHeaderField("Link")
        if (link == null || !link.contains('rel="next"')) {
            return null
        }
        return link.split(",")
                .find { it.contains('rel="next"') }
                .split(";")[0]
                .replace('<', '')
                .replace('>', '')
                .trim()
    }
}
