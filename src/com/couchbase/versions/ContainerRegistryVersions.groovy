package com.couchbase.versions

import com.couchbase.context.environments.Environment
import com.couchbase.perf.sdk.stages.PrebuiltPerformerImage
import com.couchbase.perf.shared.config.PerfConfig
import com.couchbase.tools.docker.GhcrClient
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Discovers which versions of an SDK's performer to test by listing the tags
 * already published to ghcr.io/<owner>/<sdk>-fit-performer
 */
@CompileStatic
class ContainerRegistryVersions {
    @CompileDynamic
    static List<PerfConfig.Implementation> allVersions(Environment env, String language) {
        String sdk = PrebuiltPerformerImage.SDK_NAMES.get(language)
        if (sdk == null) {
            throw new UnsupportedOperationException(
                    "No prebuilt performer image is published for '${language}' yet, so its versions " +
                    "can't be discovered from the registry. Known SDKs: ${PrebuiltPerformerImage.SDK_NAMES.keySet().join(", ")}.")
        }
        String pkg = "${sdk}-fit-performer"
        String owner = PrebuiltPerformerImage.owner(language)

        String token = System.getenv("GITHUB_TOKEN")
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(
                    "GITHUB_TOKEN environment variable is required to list published performer image versions from ghcr.io (needs read:packages scope).")
        }

        def target = new GhcrClient.Target(owner: owner, pkg: pkg)
        List versionsFound = GhcrClient.fetchVersions(target, token)

        Set<String> tags = new LinkedHashSet<>()
        for (v in versionsFound) {
            def containerTags = v?.metadata?.container?.tags
            if (containerTags instanceof List) {
                tags.addAll(containerTags as List<String>)
            }
        }

        List<PerfConfig.Implementation> out = new ArrayList<>()
        for (String tag in tags) {
            if (tag == "main") {
                out.add(new PerfConfig.Implementation(language, "main", null, null, false))
            } else {
                try {
                    def parsed = ImplementationVersion.from(tag)
                    out.add(new PerfConfig.Implementation(language, parsed.toString(), null))
                } catch (Throwable err) {
                    env.log("Skipping unparseable ${language} performer image tag '${tag}': ${err}")
                }
            }
        }

        if (out.isEmpty()) {
            throw new RuntimeException(
                    "No usable performer image tags found for ${language} (ghcr.io/${owner}/${pkg}) - nothing to test.")
        }
        return out
    }
}
