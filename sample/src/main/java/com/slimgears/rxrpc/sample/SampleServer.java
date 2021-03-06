package com.slimgears.rxrpc.sample;

import com.slimgears.rxrpc.jettywebsocket.JettyWebSocketRxTransport;
import com.slimgears.rxrpc.server.EndpointRouters;
import com.slimgears.rxrpc.server.RxServer;
import com.slimgears.util.generic.ServiceResolvers;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class SampleServer {
    private final Server jetty;
    private final JettyWebSocketRxTransport.Server transportServer = JettyWebSocketRxTransport.builder().buildServer();
    private final RxServer rxServer;

    public SampleServer(int port) {
        this.jetty = createJetty(port);
        this.rxServer = RxServer.configBuilder()
                .server(transportServer) // Use jetty WebSocket-servlet based transport
                .modules(
                        EndpointRouters.moduleByName("sampleModule"),
                        new SayHelloEndpoint_RxModule(),
                        new SampleMetaEndpointImplInteger_RxModule(),
                        new SampleMetaEndpointImplSampleRequest_RxModule())
                .resolver(ServiceResolvers
                        .builder()
                        .bind(SampleEndpoint.class).to(SampleEndpointImpl.class)
                        .bind(SayHelloEndpoint.class).to(SayHelloEndpointImpl.class)
                        .build())
                .createServer();
    }

    public void start() throws Exception {
        this.jetty.start();
        this.rxServer.start();
    }

    public void stop() throws Exception {
        this.rxServer.stop();
        this.jetty.stop();
    }

    public void join() throws InterruptedException {
        this.jetty.join();
    }

    private Server createJetty(int port) {
        Server jetty = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder servletHolder = new ServletHolder(transportServer);
        context.addServlet(servletHolder, "/api/");
        jetty.setHandler(context);
        return jetty;
    }
}
