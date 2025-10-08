package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.network.NetworkUtil
import com.couchbase.tools.tags.TagProcessor

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import java.util.Base64
import java.util.regex.Pattern

class BuildDockerColumnarNodePerformer {
    private static String master = "master"

    @CompileStatic
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Node ${build}")

        if (build instanceof BuildGerrit) {
            throw new RuntimeException("Building Gerrit not currently supported for Node")
        }

        imp.dirAbsolute(path) {
            imp.log("Current dir: ${imp.currentDir()}")
            imp.dir('transactions-fit-performer') {
                imp.dir('performers/columnar/node') {
                    String regex = "^(?!.*(/node_modules/|/build/|/proto/|/scripts/)).*"
                    // It is possible the version provided is behind the version expected based on the SHA.
                    // This can happen when the SDK is in between versions.  So the latest tag might be 4.5.0,
                    // but next version will be 4.5.1 or 4.6.0 and this can be determined by looking at the version in package.json
                    def tagBuild = maybeUpdateTagVersion(imp, build)
                    TagProcessor.processTags(new File(imp.currentDir()), tagBuild, Optional.of(Pattern.compile(regex)))
                }

                if (build instanceof BuildShaVersion) {
                    dockerBuildArgs.put("BUILD_FROM_REPO", build.sha())
                } else if (build instanceof HasVersion) {
                    dockerBuildArgs.put("BUILD_FROM_VERSION", build.version())
                } else if (build instanceof BuildMain) {
                    dockerBuildArgs.put("BUILD_FROM_REPO", 'MAIN')
                } else if (build instanceof HasSha) {
                    dockerBuildArgs.put("BUILD_FROM_REPO", build.sha())
                }

                def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

                if (!onlySource) {
                    imp.log("building docker container")
                    imp.execute("docker build -f performers/columnar/node/Dockerfile -t $imageName $serializedBuildArgs .", false, true, true)
                }
            }
        }
    }

    @CompileStatic
    private static VersionToBuild maybeUpdateTagVersion(Environment imp, VersionToBuild build) {
        if (build instanceof BuildShaVersion) {
            String packageJsonUrl = "https://api.github.com/repos/couchbase/columnar-nodejs-client/contents/package.json?ref=${build.sha()}"
            def jsonResponse = (Map) NetworkUtil.readJson(packageJsonUrl)
            def base64Content = jsonResponse.content.toString().replaceAll("\\s", "") 
            String decodedContent = new String(Base64.decoder.decode(base64Content), "UTF-8")
            def packageJson = (Map) new JsonSlurper().parseText(decodedContent)
            String fullVersion = packageJson.version
            String pkgVersion = fullVersion.split("-")[0] // we don't care about the -dev
            def pkgBuildVersion = new BuildVersion(pkgVersion)
            if (pkgBuildVersion.implementationVersion() > build.implementationVersion()) {
                imp.log("Updating build from ${build.version()} to ${pkgBuildVersion.version()} based on package.json from SHA (${build.sha().substring(0, 7)})")
                return new BuildShaVersion(pkgBuildVersion.version(), build.sha())
            }
        }
        return build
    }
}
