package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerAnalyticsJvmPerformer {
    /**
     * @param path absolute path to above 'transactions-fit-performer'
     * @param sdkLanguage 'java', 'kotlin', 'scala'
     */
    static void build(
            Environment env,
            String path,
            String sdkLanguage,
            VersionToBuild build,
            String imageName,
            boolean onlySource = false
    ) {
        env.log("Building analytics-${sdkLanguage} ${build}")

        if (onlySource) throw new UnsupportedOperationException("This performer doesn't use source transformations.")

        def dockerBuildArgs = [
                "SDK_LANG"   : sdkLanguage,
                "SDK_GIT_REV": gitRev(build),
        ]
        def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

        env.dirAbsolute(path) {
            env.dir('transactions-fit-performer') {
                env.execute("docker build --no-cache -f performers/analytics/jvm/Dockerfile $serializedBuildArgs -t $imageName .", false, true, true)
            }
        }
    }

    private static String gitRev(VersionToBuild build) {
        if (build instanceof HasGerrit) throw new UnsupportedOperationException("This performer does not support building from Gerrit.")

        // Check HasSha _before_ HasVersion because BuildShaVersion implements both.
        if (build instanceof HasSha) return build.sha()
        if (build instanceof HasVersion) return build.version()
        if (build instanceof BuildMain) return "main"
        throw new UnsupportedOperationException("unrecognized build type: ${build}")
    }

}
