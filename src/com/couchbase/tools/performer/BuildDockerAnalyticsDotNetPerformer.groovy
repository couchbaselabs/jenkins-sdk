package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.versions.ImplementationVersion
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerAnalyticsDotNetPerformer {
    /**
     * Builds a Docker image for the .NET Analytics SDK performer.
     *
     * @param env the build environment
     * @param path absolute path to above 'transactions-fit-performer'
     * @param build what to build
     * @param imageName name for the Docker image
     * @param onlySource if true, only process source (not supported for this performer)
     */
    static void build(
            Environment env,
            String path,
            VersionToBuild build,
            String imageName,
            boolean onlySource = false,
            Map<String, String> extraDockerBuildArgs = [:]
    ) {
        env.log("Building analytics-dotnet ${build}")

        if (onlySource) throw new UnsupportedOperationException("This performer doesn't use source transformations.")

        // Determine the .NET SDK version based on the Analytics SDK version
        var dotnetVersion = "10.0"
        if (build instanceof HasVersion && build.implementationVersion().isBelow(ImplementationVersion.from("1.0.0"))) {
            // Future-proofing: if we ever need older .NET versions for older SDK versions
            dotnetVersion = "8.0"
        }

        def dockerBuildArgs = [
                "FIT_DOTNET_VERSION": dotnetVersion,
                "SDK_GIT_REV"       : gitRev(build),
        ]
        if (extraDockerBuildArgs) {
            dockerBuildArgs.putAll(extraDockerBuildArgs)
        }
        
        def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

        env.dirAbsolute(path) {
            env.dir('transactions-fit-performer') {
                env.execute("docker build --no-cache -f performers/analytics/dotnet/Dockerfile $serializedBuildArgs -t $imageName .", false, true, true)
            }
        }
    }

    private static String gitRev(VersionToBuild build) {
        if (build instanceof HasGerrit) throw new UnsupportedOperationException("This performer does not support building from Gerrit.")

        // Check HasSha _before_ HasVersion because BuildShaVersion implements both.
        if (build instanceof HasSha) return build.sha()
        if (build instanceof HasVersion) return "tags/${build.version()}"
        if (build instanceof BuildMain) return "master"
        throw new UnsupportedOperationException("unrecognized build type: ${build}")
    }
}
