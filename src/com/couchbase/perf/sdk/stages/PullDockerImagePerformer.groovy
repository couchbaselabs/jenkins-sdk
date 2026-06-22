package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.perf.shared.config.PerfConfig
import com.couchbase.stages.Stage
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class PullDockerImagePerformer extends Stage {
    private static final String REGISTRY = "ghcr.io"

    private final String imageName
    private final PerfConfig.Implementation impl

    PullDockerImagePerformer(String imageName, PerfConfig.Implementation impl) {
        this.imageName = imageName
        this.impl = impl
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
        impl.image = readImageMetadata(ctx)
    }

    @CompileDynamic
    private Map<String, Object> readImageMetadata(StageContext ctx) {
        def labels = new JsonSlurper().parseText(ctx.env.executeSimple("docker inspect ${imageName}"))[0]?.Config?.Labels
        if (!(labels instanceof Map)) {
            throw new RuntimeException("docker inspect ${imageName} returned no Config.Labels; cannot record performer image metadata")
        }
        // created is the performer build time and orders builds chronologically; the base-image build-date label must not be used.
        def created = labels["org.opencontainers.image.created"]
        if (created == null) {
            throw new RuntimeException("Prebuilt image ${imageName} is missing the org.opencontainers.image.created label needed to order builds")
        }
        return [
                "created" : created,
                "pr"      : labels["com.couchbase.pr"],
                "revision": labels["org.opencontainers.image.revision"],
                "ciRun"   : labels["com.couchbase.github.actions.run"],
                "source"  : labels["org.opencontainers.image.source"],
        ]
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
