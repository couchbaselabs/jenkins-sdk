package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.stages.Stage
import groovy.transform.CompileStatic

@CompileStatic
class PullDockerImagePerformer extends Stage {
    private static final String REGISTRY = "ghcr.io"

    private final String imageName

    PullDockerImagePerformer(String imageName) {
        this.imageName = imageName
    }

    @Override
    String name() {
        return "Pull docker image ${imageName}"
    }

    @Override
    void executeImpl(StageContext ctx) {
        String token = System.getenv("GITHUB_TOKEN")
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("GITHUB_TOKEN environment variable is required to pull prebuilt performer images from ${REGISTRY} (needs read:packages scope).")
        }
        String username = System.getenv("GITHUB_ACTOR")
        if (username == null || username.trim().isEmpty()) {
            username = "couchbase"
        }
        login(ctx, REGISTRY, username, token)
        ctx.env.execute("docker pull ${imageName}", false, true, true)
    }

    private static void login(StageContext ctx, String registry, String username, String token) {
        ctx.env.log("Logging in to ${registry} as ${username}")
        def pb = new ProcessBuilder("docker", "login", registry, "-u", username, "--password-stdin")
        pb.redirectErrorStream(true)
        Process proc = pb.start()
        proc.outputStream.withWriter("UTF-8") { it.write(token) }
        String output = proc.inputStream.getText("UTF-8")
        int code = proc.waitFor()
        if (code != 0) {
            throw new RuntimeException("docker login to ${registry} failed with exit code ${code}: ${output}")
        }
    }
}
