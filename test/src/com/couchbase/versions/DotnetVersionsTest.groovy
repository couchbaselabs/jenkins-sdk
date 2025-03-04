package com.couchbase.versions

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.assertFalse

class DotNetVersionsTest {

    @Test
    void testGetAllReleases() {
        Set<ImplementationVersion> allReleases = DotNetVersions.getAllReleases()

        ImplementationVersion version1 = new ImplementationVersion(3, 4, 5, "-rc2")
        ImplementationVersion version2 = new ImplementationVersion(3, 4, 10, "-rc1")

        assertFalse(allReleases.contains(version1), "Version 3.4.5-rc2 should be filtered out")
        assertFalse(allReleases.contains(version2), "Version 3.4.10-rc1 should be filtered out")
    }
}