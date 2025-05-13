package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.stages.Stage
import com.couchbase.tools.performer.BuildDockerAnalyticsGoPerformer
import com.couchbase.tools.performer.VersionToBuildUtil
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerAnalyticsGoSDKPerformer extends Stage{

    private final String sdkVersion
    final String imageName

    static String genImageName(String sdkVersion) {
        return "performer-Analytics-go" + sdkVersion
    }

    BuildDockerAnalyticsGoSDKPerformer(String sdkVersion) {
        this(BuildDockerAnalyticsGoSDKPerformer.genImageName(sdkVersion), sdkVersion)
    }

    BuildDockerAnalyticsGoSDKPerformer(String imageName, String sdkVersion) {
        this.sdkVersion = sdkVersion
        this.imageName = imageName
    }

    @Override
    String name() {
        return "Building image ${imageName}"
    }

    @Override
    void executeImpl(StageContext ctx) {
        BuildDockerAnalyticsGoPerformer.build(ctx.env, ctx.sourceDir(), VersionToBuildUtil.from(sdkVersion, null), imageName)
    }

    String getImageName(){
        return imageName
    }
}
