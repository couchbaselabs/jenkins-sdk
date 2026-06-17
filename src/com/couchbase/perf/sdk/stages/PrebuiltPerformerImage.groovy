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
            "C++"   : "cpp",
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
        if (impl.isMain() || impl.isSnapshot) {
            return "main"
        }
        if (impl.isGerrit()) {
            return impl.version().replaceAll("[^A-Za-z0-9.]", "-")
        }
        return impl.version()
    }
}
