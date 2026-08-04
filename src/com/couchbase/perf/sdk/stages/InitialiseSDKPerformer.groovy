package com.couchbase.perf.sdk.stages

import com.couchbase.stages.Stage
import groovy.transform.CompileStatic
import com.couchbase.context.StageContext
import com.couchbase.perf.shared.config.PerfConfig

/**
 * Builds, copies (if needed), and runs a performer
 */
@CompileStatic
class InitialiseSDKPerformer extends Stage {
    public static final String CONTAINER_NAME = "performer"

    private PerfConfig.Implementation impl
    private int port = 8060
    private String imageName

    InitialiseSDKPerformer(PerfConfig.Implementation impl) {
        this.impl = impl
        if (impl.port != null) {
            port = impl.port
        }
    }

    @Override
    String name() {
        return "Init performer $impl"
    }

    @Override
    List<Stage> stagesPre(StageContext ctx) {
        if (impl.port != null) {
            // Nothing to do
            return []
        }
        else if (PrebuiltPerformerImage.isSupported(impl)) {
            String prebuiltImage = PrebuiltPerformerImage.imageName(impl)
            imageName = prebuiltImage
            return produceStages(ctx, null, prebuiltImage)
        }
        // Python and Node don't publish prebuilt performer images, so they alone are still built from
        // source here.  Every other SDK runs exclusively from its prebuilt image.
        else if (impl.language == "Python") {
            def stage1 = new BuildDockerPythonSDKPerformer(impl.version(), impl.sha)
            imageName = stage1.imageName
            return produceStages(ctx, stage1, stage1.getImageName())
        }
        else if (impl.language == "Node") {
            def stage1 = new BuildDockerNodeSDKPerformer(impl.version(), impl.sha)
            imageName = stage1.imageName
            return produceStages(ctx, stage1, stage1.getImageName())
        }
        else {
            throw new IllegalArgumentException("Unknown performer ${impl.language}")
        }
    }

    @Override
    void executeImpl(StageContext ctx) {}

    int port() {
        return port
    }

    List<Stage> produceStages(StageContext ctx, Stage stage1, String imageName){
        List<Stage> stages = []

        if (stage1 == null) {
            stages.add(new PullDockerImagePerformer(imageName, impl))
        }
        else if (!ctx.skipPerformerDockerBuild()) {
            stages.add(stage1)
        }

        if (ctx.performerServer == "localhost") {
            if (impl.language == "C++") {
                stages.add(new StartDockerImageCppSDKPerformer(imageName, CONTAINER_NAME, port, impl.version()))
            } else {
                stages.add(new StartDockerImagePerformer(imageName, CONTAINER_NAME, port, impl.version()))
            }
        } else {
            throw new IllegalArgumentException("Cannot handle running on performer remote server")
        }

        return stages
    }
}