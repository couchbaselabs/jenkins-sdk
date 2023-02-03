package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.transform.Memoized


class CppVersions {
    @Memoized
    static String getLatestSha() {
        def json = NetworkUtil.readJson("https://api.github.com/repos/couchbaselabs/couchbase-cxx-client/commits/main")
        String sha = json.sha
        return sha.substring(0, 7)
    }

    @Memoized
    static String getLatestSnapshotLabel() {
        def releases = getAllReleases()
        def closestRelease = null
        def minNonNegativeAheadBy = Integer.MAX_VALUE

        for (release in releases) {
            def compareJson = NetworkUtil.readJson("https://api.github.com/repos/couchbaselabs/couchbase-cxx-client/compare/${release}...main")
            int aheadBy = compareJson.ahead_by
            if (aheadBy >= 0 && aheadBy < minNonNegativeAheadBy) {
                minNonNegativeAheadBy = aheadBy
                closestRelease = release
            }
        }

        if (minNonNegativeAheadBy == 0) {
            return closestRelease.toString();
        } else {
            def commitJson = NetworkUtil.readJson("https://api.github.com/repos/couchbaselabs/couchbase-cxx-client/commits/main")
            String sha = commitJson.sha
            return "${closestRelease}+${minNonNegativeAheadBy}.${sha.substring(0, 7)}"
        }
    }

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        def out = new HashSet<ImplementationVersion>()
        def json = NetworkUtil.readJson("https://api.github.com/repos/couchbaselabs/couchbase-cxx-client/tags")

        for (doc in json) {
            String version = doc.name
            try {
                out.add(ImplementationVersion.from(version))
            }
            catch (err) {
                System.err.println("Failed to add C++ version ${doc}")
            }
        }

        return out
    }

    public static void main(String[] args) {
        System.out.println(getLatestSnapshotLabel());
    }
}
