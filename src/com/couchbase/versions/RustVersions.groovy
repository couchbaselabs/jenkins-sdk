package com.couchbase.versions

import groovy.transform.Memoized

class RustVersions {
    private final static String REPO = "couchbaselabs/couchbase-rs"
    private final static String BRANCH = "main"
    // Rust release tags use a "v" prefix, which must be removed for proper version parsing
    private final static Closure<String> STRIP_V_PREFIX = { name ->
        name.startsWith("v") ? name.substring(1) : name
    }

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        var allReleases = GithubVersions.getAllReleases(REPO, STRIP_V_PREFIX).findAll()
        // We want to exclude non-native Rust SDK releases, which are all pre-1.0.0
        return allReleases.findAll { version -> version.major != 0 }
    }

    static ImplementationVersion getLatestSnapshot() {
        return getSnapshot(BRANCH, true)
    }

    static ImplementationVersion getSnapshot(String shaOrBranch, boolean nextReleaseIsDotMinor) {
        def attrs = GithubVersions.getSnapshotAttributes(REPO, shaOrBranch, STRIP_V_PREFIX)
        return attrs.toImplementationVersion(nextReleaseIsDotMinor)
    }
}
