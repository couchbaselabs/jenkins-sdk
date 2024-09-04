package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.stages.Stage
import com.couchbase.tools.performer.BuildDockerColumnarNodePerformer
import com.couchbase.tools.performer.BuildDockerNodePerformer
import com.couchbase.tools.performer.VersionToBuildUtil


class BuildDockerColumnarNodeSDKPerformer extends Stage {
    private final String imageName
    private final String sdkVersion
    private final String sha

    static String genImageName(String sdkVersion) {
        return "performer-columnar-node" + sdkVersion
    }

  BuildDockerColumnarNodeSDKPerformer(String imageName, String sdkVersion, String sha) {
        this.sdkVersion = sdkVersion
        this.imageName = imageName
        this.sha = sha
    }

  BuildDockerColumnarNodeSDKPerformer(String sdkVersion, String sha) {
        this(BuildDockerColumnarNodeSDKPerformer.genImageName(sdkVersion), sdkVersion, sha)
    }

    @Override
    String name() {
        return "Building image ${imageName}"
    }

    @Override
    protected void executeImpl(StageContext ctx) {
        ctx.env.log("Building node snapshot image")
      BuildDockerColumnarNodePerformer.build(ctx.env, ctx.sourceDir(), VersionToBuildUtil.from(sdkVersion, sha), imageName)
    }

    String getImageName() {
        return this.imageName
    }
}
