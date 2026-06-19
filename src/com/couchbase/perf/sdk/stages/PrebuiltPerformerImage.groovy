package com.couchbase.perf.sdk.stages

import com.couchbase.perf.shared.config.PerfConfig.Implementation
import groovy.transform.CompileStatic

@CompileStatic
class PrebuiltPerformerImage {
    private static final String REGISTRY = "ghcr.io"
    private static final String ORG = "couchbase"

    private static final Map<String, String> SDK_NAMES = [
            "Java"  : "java",
            "Scala" : "scala",
            "Kotlin": "kotlin",
            ".NET"  : "dotnet",
            "Go"    : "go",
            "Python": "python",
            "Node"  : "node",
            "C++"   : "cxx",
            "Ruby"  : "ruby",
            "Rust"  : "rust",
    ]

    static String imageName(Implementation impl) {
        String sdk = SDK_NAMES.get(impl.language)
        if (sdk == null) {
            throw new IllegalArgumentException("No prebuilt FIT performer image is known for SDK '${impl.language}'. Known SDKs: ${SDK_NAMES.keySet().join(", ")}.")
        }
        return "${REGISTRY}/${ORG}/${sdk}-fit-performer:${tag(impl)}"
    }

    static String tag(Implementation impl) {
        if (impl.isGerrit()) {
            throw new IllegalArgumentException("Prebuilt FIT performer images are not supported for Gerrit changesets (${impl.version()}). Disable usePrebuiltImages to build from source.")
        }
        if (impl.isMain() || impl.isSnapshot) {
            return "main"
        }
        return impl.version()
    }
}
