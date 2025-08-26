package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.transform.Memoized


class RustVersions {
    private final static String REPO = "couchbase/couchbase-rs"

    @Memoized
    static String getLatestCargoEntry() {
        def json = NetworkUtil.readJson("https://crates.io/api/v1/crates/couchbase")

        return json.max_stable_version
    }

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        return GithubVersions.getAllReleases(REPO)
    }
}
