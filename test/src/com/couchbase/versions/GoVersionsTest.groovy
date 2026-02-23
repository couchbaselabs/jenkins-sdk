package com.couchbase.versions

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertTrue

class GoVersionsTest {
    @Test
    void canGetReleases() {
        def releases = GoVersions.getAllReleases()

        // Just check that some known releases are included
        assertTrue(releases.contains(ImplementationVersion.from("2.12.0")))
        assertTrue(releases.contains(ImplementationVersion.from("2.11.3")))
        assertTrue(releases.contains(ImplementationVersion.from("2.3.0")))
    }
}
