package com.couchbase.perf.sdk.stages

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import com.couchbase.context.StageContext
import com.couchbase.stages.Stage

@CompileStatic
class StartDockerImageCppSDKPerformer extends Stage {

    private String imageName
    private String containerName
    private int port
    private final String version

    StartDockerImageCppSDKPerformer(String imageName, String containerName, int port, String version) {
        this.version = version
        this.imageName = imageName
        this.containerName = containerName
        this.port = port
    }

    @Override
    String name() {
        return "Start docker image $imageName with name ${containerName} on $port"
    }

    @Override
    void executeImpl(StageContext ctx) {
        // -d so this will run in background
        ctx.env.execute("${ctx.env.isWindows() || ctx.env.isMacOS() ? "" : "timeout 24h "}docker run --rm -d --network perf -p $port:8060 --name ${containerName} $imageName",
                false, false, true)
        // Stream the logs in the background
        ctx.env.execute("docker logs --follow ${containerName}", false, false, true, true)
        // Neither of the commands above will block
    }

    @Override
    void finishImpl(StageContext ctx) {
        try {
            // By default, allow 30 seconds for the container to shutdown in case we need to wait for a core dump to be parsed
            Integer count = 6
            // TODO:  Make this configurable
            // def timeout = System.getenv("CPP_KILL_CONTAINER_TIMEOUT")
            // if (timeout != null) {
            //     count = Integer.parseInt(timeout).intdiv(5)
            // }            
            boolean isRunning = isDockerContainerRunning(ctx)
            if(isRunning){
                ctx.env.log("Allowing ${count * 5} seconds for container to shutdown...")
            }
            while( count > 0 && isRunning ) {
                ctx.env.log("Container ${containerName} is still running, waiting for 5 seconds...")
                sleep(5000)
                isRunning = isDockerContainerRunning(ctx)
                count--
            }
            if(isRunning) {
                ctx.env.executeSimple("docker kill ${containerName}")
            }
        }
        catch (RuntimeException err) {
            // Probably just failed to build the performer, so continue
            ctx.env.log("Failed to stop ${containerName} with err ${err}, continuing")
        }
    }

    boolean isDockerContainerRunning(StageContext ctx) {
        String dockerCmd = "docker ps --format json"
        String dockerOutput = ctx.env.execute(dockerCmd, false)
        def parser = new JsonSlurper()
        def containers = dockerOutput.split("\n")
        
        for (container in containers) {
            def json = parser.parseText(container)
            assert json instanceof Map
            if (json.Names == containerName && json.State == "running") {
                return true
            }
        }
        return false
    }
}