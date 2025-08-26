package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

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
                    TagProcessor.processTags(new File(imp.currentDir()), build)
                }
                if (!onlySource) {
                    if (build instanceof BuildMain) {
                        imp.execute("docker build -f performers/rust/Dockerfile -t $imageName .", false, true, true)
                    }
                    else if (build instanceof HasSha) {
                        imp.execute("docker build -f performers/rust/Dockerfile -t $imageName --build-arg SDK_BRANCH=${build.sha()} .", false, true, true)
                    }
                    else if (build instanceof HasVersion) {
                        imp.execute("docker build -f performers/rust/Dockerfile -t $imageName --build-arg SDK_TAG=${build.version()} .", false, true, true)
                    }
                }
            }
        }
    }
}
