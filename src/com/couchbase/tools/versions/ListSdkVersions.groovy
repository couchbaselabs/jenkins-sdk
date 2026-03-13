package com.couchbase.tools.versions

import com.couchbase.tools.performer.Sdk
import com.couchbase.tools.performer.SdkSynonyms
import com.couchbase.versions.CppVersions
import com.couchbase.versions.DotNetVersions
import com.couchbase.versions.GoVersions
import com.couchbase.versions.ImplementationVersion
import com.couchbase.versions.JVMVersions
import com.couchbase.versions.NodeVersions
import com.couchbase.versions.PythonVersions
import com.couchbase.versions.RubyVersions
import com.couchbase.versions.RustVersions
import groovy.cli.picocli.CliBuilder

class ListSdkVersions {
    static void main(String[] args) {
        def cli = new CliBuilder()
        cli.with {
            s(longOpt: 'sdk', args: 1, required: true, 'SDK to list versions for (java-sdk, scala, kotlin, go, python, c++, .net, ruby, rust)')
        }

        def options = cli.parse(args)
        if (!options) {
            System.exit(-1)
        }

        String sdkRaw = options.s.toLowerCase().trim()
        def sdk = SdkSynonyms.sdk(sdkRaw)

        Set<ImplementationVersion> versions = new HashSet<>()

        switch (sdk) {
            case Sdk.JAVA:
                versions = JVMVersions.getAllJVMReleases("java-client")
                break
            case Sdk.SCALA:
                versions = JVMVersions.getAllJVMReleases("scala-client_2.12")
                break
            case Sdk.KOTLIN:
                versions = JVMVersions.getAllJVMReleases("kotlin-client")
                break
            case Sdk.JAVA_COLUMNAR:
                versions = JVMVersions.getAllJVMReleases("columnar-java-client")
                break
            case Sdk.GO:
                versions = GoVersions.allReleases
                break
            case Sdk.PYTHON:
                versions = PythonVersions.allReleases
                break
            case Sdk.CPP:
                versions = CppVersions.allReleases
                break
            case Sdk.NODE:
                versions = NodeVersions.allReleases
                break
            case Sdk.DOTNET:
                versions = DotNetVersions.allReleases
                break
            case Sdk.RUBY:
                versions = RubyVersions.allReleases
                break
            case Sdk.RUST:
                versions = RustVersions.allReleases
                break
            default:
                println "Listing versions for ${sdkRaw} is not perfectly supported or implemented in this tool yet."
                System.exit(-1)
        }

        versions.sort().collect { it.toString() }
                .each { version ->
                    println version
                }
    }
}
