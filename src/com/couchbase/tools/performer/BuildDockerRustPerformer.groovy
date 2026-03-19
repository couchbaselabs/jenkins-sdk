package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

import java.util.regex.Pattern

class BuildDockerRustPerformer {
    private static String master = "main"

    @CompileStatic
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false) {
        imp.log("Building Rust ${build}")

        if (build instanceof BuildGerrit) {
            throw new RuntimeException("Building Gerrit not currently supported for Rust")
        }

        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer') {
                imp.dir("performers/rust") {
                    TagProcessor.processTags(new File(imp.currentDir()), build, Optional.of(Pattern.compile(".*\\.rs")))
                }
                if (!onlySource) {
                    Map<String, String> dockerBuildArgs = [:]

                    if (build instanceof BuildMain) {
                        // no extra build args
                    }
                    else if (build instanceof HasSha) {
                        dockerBuildArgs.put('SDK_SHA', build.sha())
                    }
                    else if (build instanceof HasVersion) {
                        dockerBuildArgs.put('SDK_TAG', 'v' + build.version())
                    }

                    imp.dockerBuild("-f performers/rust/Dockerfile -t $imageName .", dockerBuildArgs)
                }
            }
        }
    }
}
