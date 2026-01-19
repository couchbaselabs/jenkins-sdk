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
                    def tagBuild = maybeUpdateTagVersion(imp, build)
                    TagProcessor.processTags(new File(imp.currentDir()), tagBuild, Optional.of(Pattern.compile(".*\\.go")))
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
                    imp.execute("docker build -f performers/go/Dockerfile $serializedBuildArgs -t $imageName .", false, true, true)
                }
            }
        }
    }

    @CompileStatic
    private static VersionToBuild maybeUpdateTagVersion(Environment imp, VersionToBuild build) {
        if (build instanceof HasSha) {
            String packageJsonUrl = "https://proxy.golang.org/github.com/couchbase/gocb/v2/@v/${build.sha()}.info"
            def jsonResponse = (Map) NetworkUtil.readJson(packageJsonUrl)
            def version = jsonResponse["Version"] as String

            String pkgVersion = version.split("-")[0] // we don't care about the bit after - so we'll get e.g. v2.11.1
            def pkgBuildVersion = new BuildVersion(pkgVersion.substring(1, pkgVersion.length())) // remove leading 'v'
            imp.log("Updating build to ${pkgBuildVersion.version()} based on package.json from SHA (${build.sha().substring(0, 7)})")

            return new BuildShaVersion(pkgBuildVersion.version(), build.sha())
        }

        return build
    }
}
