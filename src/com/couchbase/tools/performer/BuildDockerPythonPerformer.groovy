package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import com.couchbase.versions.ImplementationVersion
import groovy.transform.CompileStatic

import java.security.cert.CertificateEncodingException
import java.util.logging.Logger
import java.util.regex.Pattern

@CompileStatic
class BuildDockerPythonPerformer {
    private static Logger logger = Logger.getLogger("")

    /**
     * @param imp        the build environment
     * @param path       absolute path to above 'transactions-fit-performer'
     * @param build what to build
     * @param imageName  the name of the docker image
     * @param onlySource whether to skip the docker build
     */
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Python ${build}")

        if (build instanceof BuildGerrit) {
            throw new RuntimeException("Building Gerrit not currently supported for Python")
        }

        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer') {
                imp.dir('performers/python') {
                    TagProcessor.processTags(new File(imp.currentDir()), build, Optional.of(Pattern.compile(".*\\.py")))
                }

                if (build instanceof HasVersion) {
                    dockerBuildArgs.put("BUILD_FROM_VERSION", build.version())
                } else if (build instanceof BuildMain) {
                    dockerBuildArgs.put("BUILD_FROM_REPO", 'MAIN')
                } else if (build instanceof HasSha) {
                    dockerBuildArgs.put("BUILD_FROM_REPO", build.sha())
                }

                if (!onlySource) {
                    imp.dockerBuild("-f ./performers/python/Dockerfile -t $imageName .", dockerBuildArgs)
                }
            }
        }
    }
}
