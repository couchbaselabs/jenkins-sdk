package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.transform.Memoized


class DotNetVersions {
    private final static String REPO = "couchbase/couchbase-net-client"
    private final static String BRANCH = "master"

    @Memoized
    static String getLatestSha() {
        return GithubVersions.getLatestSha(REPO, BRANCH)
    }

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        var allVersions = GithubVersions.getAllReleases(REPO)
        var skipVersions = [
                new ImplementationVersion(3, 4, 10, "rc1"),
                new ImplementationVersion(3, 4, 5, "rc2")
        ]
        return allVersions.findAll {it -> !skipVersions.contains(it) }
    }
}
