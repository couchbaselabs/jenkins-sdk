package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class BuildDockerColumnarGoPerformer {
    private static final String VERSION_ARG = "SDK_VERSION"

    /**
     * @param path absolute path to above 'transactions-fit-performer'
     * @param build what to build
     */
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Columnar Go ${build}")

        // Build context needs to be perf-sdk as we need the .proto files
        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer') {
                imp.dir('performers/columnar/go') {
                    TagProcessor.processTags(new File(imp.currentDir()), build, Optional.of(Pattern.compile(".*\\.go")))
                }

                // We check HasSha and HasGerrit _before_ HasVersion, since BuildShaVersion & BuildGerritVersion
                // implement HasVersion as well as HasSha/HasGerrit
                if (build instanceof HasSha) {
                    dockerBuildArgs.put(VERSION_ARG, build.sha())
                }
                else if (build instanceof HasGerrit) {
                    throw new RuntimeException("Building Gerrit not currently supported")
                }
                else if (build instanceof HasVersion) {
                    dockerBuildArgs.put(VERSION_ARG, "v${build.version()}".toString())
                }
                else if (build instanceof BuildMain) {
                    dockerBuildArgs.put(VERSION_ARG, "main")
                }


                def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

                if (!onlySource) {
                    imp.execute("docker build -f performers/columnar/go/Dockerfile --platform=linux/amd64 $serializedBuildArgs -t $imageName .", false, true, true)
                }
            }
        }
    }
}
