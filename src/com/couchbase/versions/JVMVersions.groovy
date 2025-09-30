package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.transform.Memoized


class JVMVersions {
    @Memoized
    static ImplementationVersion getLatestSnapshotBuild(String client) {
        def host = "s01.oss.sonatype.org"
        def snapshots = null
        try {
            snapshots = NetworkUtil.readXml("https://${host}/content/repositories/snapshots/com/couchbase/client/${client}/maven-metadata.xml")
        }
        catch (Throwable err) {
            throw new RuntimeException("Unable to fetch snapshot metadata for ${client} from https://${host}", err)
        }

        // "latest" doesn't look up to date so assuming list will always be time-ordered
        def lastSnapshot = snapshots.versioning.versions.childNodes()[snapshots.versioning.versions.childNodes().size() - 1].text()

        def artifactXml = null
        try {
            artifactXml = NetworkUtil.readXml("https://${host}/content/repositories/snapshots/com/couchbase/client/${client}/${lastSnapshot}/maven-metadata.xml")
        }
        catch (Throwable err) {
            throw new RuntimeException("Unable to fetch artifact metadata for ${client} ${lastSnapshot} from https://${host}", err)
        }

        // "20220715.074746-6"
        def timestamp = artifactXml.versioning.snapshot.timestamp
        def builderNumber = artifactXml.versioning.snapshot.buildNumber
        def version = ImplementationVersion.from(lastSnapshot)
        def out = ImplementationVersion.from("${version.major}.${version.minor}.${version.patch}-${timestamp}-${builderNumber}")
        return out
    }

    @Memoized
    static Set<ImplementationVersion> getAllJVMReleases(String client) {
        int start = 0
        def out = new HashSet<ImplementationVersion>()

        while (true) {
            String url = "https://search.maven.org/solrsearch/select?q=g:com.couchbase.client+AND+a:${client}&start=${start}&core=gav&rows=20&wt=json"
            def json = NetworkUtil.readJson(url)
            def docs = json.response.docs

            if (docs.size() == 0) {
                break
            }
            else {
                for (doc in docs) {
                    String version = doc.v
                    try {
                        out.add(ImplementationVersion.from(version))
                    }
                    catch (err) {
                        System.err.println("Failed to add version ${client} ${doc}")
                    }
                }
                start += docs.size()
            }
        }

        return out
    }
}
