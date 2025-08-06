package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerAnalyticsNodePerformer {
    private static String REPO = "github:couchbase/analytics-nodejs-client"

    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Node Analytics ${build}")

        if (build instanceof BuildGerrit) {
            throw new RuntimeException("Building Gerrit is not supported for Node")
        }
        if (build instanceof HasVersion) {
            throw new RuntimeException("Building Version is not supported before release")
        }
        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer/performers/analytics/node') {
                writePackageFile(imp, build)
                TagProcessor.processTags(new File(imp.currentDir()), build)
            }

            def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

            if (!onlySource) {
                imp.execute("docker build -f ./transactions-fit-performer/performers/analytics/node/Dockerfile -t $imageName $serializedBuildArgs .", false, true, true)
            }
        }
    }

    @CompileStatic
    private static void writePackageFile(Environment imp, VersionToBuild build) {
        def packageFile = new File("${imp.currentDir()}/package.json")
        def lines = packageFile.readLines()
        packageFile.write("")

        for (String line : lines) {
            if (line.contains("couchbase-analytics")) {
                if (build instanceof HasVersion) {
                    packageFile.append("\t\"couchbase-analytics\": \"${build.version()}\",\n")
                } else if (build instanceof BuildMain) {
                    packageFile.append("\t\"couchbase-analytics\": \"${REPO}\",\n")
                } else if (build instanceof HasSha) {
                    packageFile.append("\t\"couchbase-analytics\": \"${REPO}#${build.sha()}\",\n")
                } else {
                    packageFile.append(line + "\n")
                }
            } else {
                packageFile.append(line + "\n")
            }
        }
    }
}
