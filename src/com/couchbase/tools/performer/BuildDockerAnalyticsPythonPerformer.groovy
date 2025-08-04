package com.couchbase.tools.performer

import com.couchbase.context.environments.Environment
import com.couchbase.tools.tags.TagProcessor
import groovy.transform.CompileStatic

import java.util.logging.Logger
import java.util.regex.Pattern

@CompileStatic
class BuildDockerAnalyticsPythonPerformer {
    private static Logger logger = Logger.getLogger("")

    /**
     * @param imp        the build environment
     * @param path       absolute path to above 'transactions-fit-performer'
     * @param build what to build
     * @param imageName  the name of the docker image
     * @param onlySource whether to skip the docker build
     */
    static void build(Environment imp, String path, VersionToBuild build, String imageName, boolean onlySource = false, Map<String, String> dockerBuildArgs = [:]) {
        imp.log("Building Python Analytics ${build}")

        if (build instanceof BuildGerrit) {
            throw new RuntimeException("Building Gerrit not currently supported for Python")
        }

        imp.dirAbsolute(path) {
            imp.dir('transactions-fit-performer/performers/analytics/python') {
                // Explanation of the regex:
                // ^                                 - Start of the string (path)
                // (?!.*[/\\]\.venv[/\\])           - Negative Lookahead: Ensure the path does NOT contain "/.venv/"
                //                                     (?:.*) matches any characters (non-greedy)
                //                                     [/\\] matches either / or \ (for path separators)
                // (?!.*[/\\]protocol[/\\])         - Negative Lookahead: Ensure the path does NOT contain "/protocol/"
                // .* - Match any characters (the actual path content)
                // \.py                              - Match ".py" literally (dot needs escaping)
                String regex = "^(?!.*[\\\\/]\\.venv[\\\\/])(?!.*[\\\\/](?:protocol)[\\\\/]).*\\.py"
                TagProcessor.processTags(new File(imp.currentDir()), build, Optional.of(Pattern.compile(regex)))
            }

            if (build instanceof HasVersion) {
                dockerBuildArgs.put("BUILD_FROM_VERSION", build.version())
            } else if (build instanceof BuildMain) {
                dockerBuildArgs.put("BUILD_FROM_REPO", 'MAIN')
            } else if (build instanceof HasSha) {
                dockerBuildArgs.put("BUILD_FROM_REPO", build.sha())
            }

            def serializedBuildArgs = dockerBuildArgs.collect((k, v) -> "--build-arg $k=$v").join(" ")

            if (!onlySource) {
                imp.execute("docker build -f ./transactions-fit-performer/performers/analytics/python/Dockerfile -t $imageName $serializedBuildArgs .", false, true, true)
            }
        }
    }
}
