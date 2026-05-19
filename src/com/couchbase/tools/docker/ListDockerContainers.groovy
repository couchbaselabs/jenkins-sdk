package com.couchbase.tools.docker

import com.couchbase.tools.docker.GhcrClient.GhcrHttpException
import com.couchbase.tools.docker.GhcrClient.PackageNotFoundException
import groovy.cli.picocli.CliBuilder
import groovy.json.JsonOutput

/**
 * CLI front-end that lists every container version, with all available
 * metadata, for a GHCR container package given its github.com package URL, e.g.
 * https://github.com/couchbaselabs/transactions-fit-performer/pkgs/container/java-fit-performer
 *
 * All registry logic lives in {@link GhcrClient} so it can be reused; this
 * class only handles argument parsing, output formatting, and exit codes.
 *
 * Requires a GITHUB_TOKEN environment variable with the read:packages scope.
 */
class ListDockerContainers {

    static void main(String[] args) {
        def cli = new CliBuilder(usage: 'listDockerContainersand  -u <ghcr-url>[,<ghcr-url>...] [--json] [--compact] [--limit N]')
        cli.with {
            u(longOpt: 'url', args: 1, required: true, 'GHCR container package URL(s), comma separated')
            json(longOpt: 'json', 'Emit raw JSON instead of human-readable text')
            compact(longOpt: 'compact', 'Print only the version tags, one per line')
            limit(longOpt: 'limit', args: 1, 'Stop after N versions (per URL)')
        }

        def options = cli.parse(args)
        if (!options) {
            System.exit(-1)
        }

        String rawUrls = options.u as String
        List<String> urls = rawUrls == null ? [] :
                rawUrls.split(",").collect { it.trim() }.findAll { !it.isEmpty() }
        if (urls.isEmpty()) {
            System.err.println("No URL supplied. Pass -Purl=<ghcr-url>[,<ghcr-url>...], e.g. " +
                    "-Purl=https://github.com/couchbaselabs/transactions-fit-performer/pkgs/container/java-fit-performer")
            System.exit(-1)
        }

        String token = System.getenv("GITHUB_TOKEN")
        if (token == null || token.trim().isEmpty()) {
            System.err.println("GITHUB_TOKEN environment variable is required (needs read:packages scope).")
            System.exit(-1)
        }

        Integer limit = options.limit ? Integer.parseInt(options.limit as String) : null

        // Preserve insertion order so output matches the order URLs were given.
        Map<String, List> versionsByUrl = new LinkedHashMap<>()
        for (String url in urls) {
            try {
                versionsByUrl.put(url, GhcrClient.fetchVersions(url, token, limit))
            }
            catch (PackageNotFoundException e) {
                System.err.println(e.message)
                System.exit(-1)
                return
            }
            catch (GhcrHttpException e) {
                System.err.println(e.message)
                System.exit(-1)
                return
            }
        }

        boolean multi = versionsByUrl.size() > 1

        if (options.json) {
            // Single URL keeps the original flat array; multiple URLs key by URL.
            def payload = multi ? versionsByUrl : versionsByUrl.values().first()
            println JsonOutput.prettyPrint(JsonOutput.toJson(payload))
        } else {
            versionsByUrl.each { u, versions ->
                if (multi) {
                    println "=" * 60
                    println u
                    println "=" * 60
                }
                if (options.compact) {
                    printCompact(versions)
                } else {
                    printHuman(versions)
                }
            }
        }
    }

    private static void printCompact(List versions) {
        versions.each { v ->
            def tags = v.metadata?.container?.tags
            if (tags) {
                tags.each { println it }
            }
        }
    }

    private static void printHuman(List versions) {
        if (versions.isEmpty()) {
            println "No container versions found."
            return
        }
        println "${versions.size()} container version(s):"
        versions.each { v ->
            println "-" * 60
            println "id:          ${v.id}"
            println "digest:      ${v.name}"
            def tags = v.metadata?.container?.tags
            println "tags:        ${tags ? tags.join(', ') : '(untagged)'}"
            println "created_at:  ${v.created_at}"
            println "updated_at:  ${v.updated_at}"
            println "html_url:    ${v.html_url}"
            println "package_url: ${v.package_html_url}"
            println "metadata:    ${JsonOutput.toJson(v.metadata)}"
        }
    }
}
