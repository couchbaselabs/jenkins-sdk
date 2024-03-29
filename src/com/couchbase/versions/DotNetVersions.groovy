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
        return GithubVersions.getAllReleases(REPO)
    }
}
