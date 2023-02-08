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
    static String getLatestSnapshotLabel(String version) {
        def json = NetworkUtil.readJson("https://api.github.com/repos/couchbaselabs/couchbase-cxx-client/commits/main")
        String sha = json.sha
        String commitDate = json.commit.committer.date
        String[] parts = commitDate.split("T")
        String date = parts[0].replaceAll("[^0-9]", "")
        String time = parts[1].replaceAll("[^0-9]", "")
        return version + "-" + date + "." + time + "-" + sha.substring(0, 7)
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
}