package com.couchbase.tools.docker

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class GhcrClientTest {
    @Test
    void parsesOwnerAndPackageFromGhcrUrl() {
        def t = GhcrClient.parseUrl(
                "https://github.com/couchbaselabs/transactions-fit-performer/pkgs/container/java-fit-performer")
        assertEquals("couchbaselabs", t.owner)
        assertEquals("java-fit-performer", t.pkg)
    }

    @Test
    void parsesUrlWithTrailingQuery() {
        def t = GhcrClient.parseUrl(
                "https://github.com/couchbaselabs/transactions-fit-performer/pkgs/container/java-fit-performer?tab=tags")
        assertEquals("couchbaselabs", t.owner)
        assertEquals("java-fit-performer", t.pkg)
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(IllegalArgumentException.class, {
            GhcrClient.parseUrl("https://github.com/couchbaselabs/transactions-fit-performer")
        })
    }
}
