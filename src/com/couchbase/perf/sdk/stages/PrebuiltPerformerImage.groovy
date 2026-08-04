package com.couchbase.perf.sdk.stages

import com.couchbase.perf.shared.config.PerfConfig.Implementation
import groovy.transform.CompileStatic

@CompileStatic
class PrebuiltPerformerImage {
    private static final String REGISTRY = "ghcr.io"
    private static final String ORG = "couchbase"

    // Python and Node do not yet have prebuilt images
    private static final Map<String, String> SDK_NAMES = [
            "Java"  : "java",
            "Scala" : "scala",
            "Kotlin": "kotlin",
            ".NET"  : "dotnet",
            "Go"    : "go",
            "C++"   : "cxx",
            "Ruby"  : "ruby",
            "Rust"  : "rust",
    ]

    /** True iff this SDK is run from a prebuilt image rather than built from source. */
    static boolean isSupported(Implementation impl) {
        return SDK_NAMES.containsKey(impl.language)
    }

    static String imageName(Implementation impl) {
        String sdk = SDK_NAMES.get(impl.language)
        if (sdk == null) {
            throw new IllegalArgumentException("No prebuilt FIT performer image is known for SDK '${impl.language}'. Known SDKs: ${SDK_NAMES.keySet().join(", ")}.")
        }
        return "${REGISTRY}/${ORG}/${sdk}-fit-performer:${tag(impl)}"
    }

    static String tag(Implementation impl) {
        if (impl.isGerrit()) {
            // Mirrors the tag couchbaselabs/sdk-docker-build-action publishes for a Gerrit ref,
            // e.g. "refs/changes/97/249597/1" -> "refs-changes-97-249597-1".
            return impl.version().replace("/", "-")
        }
        if (impl.isMain() || impl.isSnapshot) {
            return "main"
        }
        return impl.version()
    }
}
