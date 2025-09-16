package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.transform.Memoized

class RustVersions {
    private final static String REPO = "couchbaselabs/couchbase-rs"
    private final static String BRANCH = "main"

    // The Rust SDK hasn’t had an official release yet. Since it was merged into the
    // same repo/branch as the old deprecated SDK, the most recent Git tag belongs
    // to that code. To avoid counting commits from the wrong history,
    // we pin a reference commit (the first commit of the native SDK) and arbitrarily call
    // it version 0.1.0 to keep it easily sortable against future releases.
    //
    // Pre-release versions are in the form:
    //   0.1.0-<commit count since reference commit>+<SHA>
    private final static String referenceSha = "0d1a6fdfa39adff00d5960a5d907efd1f49ee1a2"
    private final static ImplementationVersion referenceVersion = ImplementationVersion.from("0.0.0")


    @Memoized
    static String getLatestCargoEntry() {
        def json = NetworkUtil.readJson("https://crates.io/api/v1/crates/couchbase")

        return json.max_stable_version
    }

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        var allReleases = GithubVersions.getAllReleases(REPO).findAll()
        // We want to exclude non-native Rust SDK releases, which are all pre-1.0.0
        return allReleases.findAll { version -> version.major != 0 }
    }

    static ImplementationVersion getLatestSnapshotPrerelease() {
        return getSnapshotPrerelease(BRANCH)
    }

    static ImplementationVersion getSnapshotPrerelease(String sha) {
        def attrs = GithubVersions.getSnapshotAttributesUsingReferenceCommit(REPO, sha, referenceSha, referenceVersion)
        return attrs.toImplementationVersion(true)
    }
}
