package com.couchbase.perf.sdk.stages

import com.couchbase.perf.shared.config.PerfConfig.Implementation
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class PrebuiltPerformerImageTest {
    @Test
    void javaImageStaysUnderCouchbase() {
        assertEquals("couchbase", PrebuiltPerformerImage.owner("Java"))
        assertEquals("ghcr.io/couchbase/java-fit-performer:3.10.1",
                PrebuiltPerformerImage.imageName(new Implementation("Java", "3.10.1", null)))
    }

    @Test
    void quarkusImagesAreUnderCouchbaselabs() {
        assertEquals("couchbaselabs", PrebuiltPerformerImage.owner("Quarkus"))
        assertEquals("couchbaselabs", PrebuiltPerformerImage.owner("Quarkus Native"))
        assertEquals("ghcr.io/couchbaselabs/quarkus-fit-performer:1.4.1",
                PrebuiltPerformerImage.imageName(new Implementation("Quarkus", "1.4.1", null)))
        assertEquals("ghcr.io/couchbaselabs/quarkus-native-fit-performer:main",
                PrebuiltPerformerImage.imageName(new Implementation("Quarkus Native", "main", null)))
    }
}
