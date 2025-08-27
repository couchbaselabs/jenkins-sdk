package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.stages.Stage
import com.couchbase.tools.performer.BuildDockerRustPerformer
import com.couchbase.tools.performer.VersionToBuildUtil
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerRustSDKPerformer extends Stage{

    private final String sdkVersion
    final String imageName

    static String genImageName(String sdkVersion) {
        return "performer-rust" + sdkVersion
    }

    BuildDockerRustSDKPerformer(String sdkVersion) {
        this(BuildDockerRustSDKPerformer.genImageName(sdkVersion), sdkVersion)
    }

    BuildDockerRustSDKPerformer(String imageName, String sdkVersion) {
        this.sdkVersion = sdkVersion
        this.imageName = imageName
    }

    @Override
    String name() {
        return "Building image ${imageName}"
    }

    @Override
    void executeImpl(StageContext ctx) {
        BuildDockerRustPerformer.build(ctx.env, ctx.sourceDir(), VersionToBuildUtil.from(sdkVersion, null), imageName)
    }

    String getImageName(){
        return imageName
    }
}
