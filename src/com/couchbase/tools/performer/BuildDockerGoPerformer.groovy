package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.network.NetworkUtil
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class BuildDockerGoPerformer {
    private static final String VERSION_ARG = "SDK_VERSION"
    private static final String GERRIT_REF_ARG = "SDK_GERRIT_REF"

    /**
     * @param path absolute path to above 'transactions-fit-performer'
     * @param build what to build
     */
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Go ${build}")

        // Build context needs to be perf-sdk as we need the .proto files
        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer') {
                imp.dir('performers/go') {
                    TagProcessor.processTags(new File(imp.currentDir()), build, Optional.of(Pattern.compile(".*\\.go")))
                }

                // We check HasSha and HasGerrit _before_ HasVersion, since BuildShaVersion & BuildGerritVersion
                // implement HasVersion as well as HasSha/HasGerrit
                if (build instanceof HasSha) {
                    dockerBuildArgs.put(VERSION_ARG, build.sha())
                }
                else if (build instanceof HasGerrit) {
                    dockerBuildArgs.put(GERRIT_REF_ARG, build.gerrit())
                }
                else if (build instanceof HasVersion) {
                    dockerBuildArgs.put(VERSION_ARG, "v${build.version()}".toString())
                }
                else if (build instanceof BuildMain) {
                    // If we set SDK_VERSION to "master", docker might cache the result of an earlier go get command result.
                    // This means we could end up using an outdated version of master. We should fetch the latest commit SHA here and provide that to prevent this.
                    def json = NetworkUtil.readJson("https://proxy.golang.org/github.com/couchbase/gocb/v2/@v/master.info")
                    dockerBuildArgs.put(VERSION_ARG, json["Origin"]["Hash"] as String)
                }


                def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

                if (!onlySource) {
                    imp.execute("docker build -f performers/go/Dockerfile --platform=linux/amd64 $serializedBuildArgs -t $imageName .", false, true, true)
                }
            }
        }
    }
}
