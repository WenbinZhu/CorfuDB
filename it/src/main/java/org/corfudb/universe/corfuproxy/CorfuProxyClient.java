package org.corfudb.universe.corfuproxy;

import com.google.protobuf.RpcCallback;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.universe.corfuproxy.Proxy.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class CorfuProxyClient {
    @NonNull
    private String host;

    @NonNull
    private Integer port;

    private Bootstrap bootstrap;

    private String clientId = "clientId-" + UUID.randomUUID();

    private final Map<String, RpcCallback<ProxyResponseMsg>> callbackMap = new ConcurrentHashMap<>();

    private class CorfuClientHandler extends SimpleChannelInboundHandler<ProxyResponseMsg> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProxyResponseMsg msg) throws Exception {
            RpcCallback<ProxyResponseMsg> callback = callbackMap.remove(msg.getMessageId());

            if (callback == null) {
                log.error("Callback not found for response message with id: {}", msg.getMessageId());
                return;
            }

            callback.run(msg);
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception during client channel handling.", cause);
            ctx.close();
        }
    }

    public CorfuProxyClient setup() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipe = ch.pipeline();
                        // Protobuf decoders
                        pipe.addLast(new ProtobufVarint32FrameDecoder());
                        pipe.addLast(new ProtobufDecoder(ProxyResponseMsg.getDefaultInstance()));
                        // Protobuf encoders
                        pipe.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipe.addLast(new ProtobufEncoder());
                        // Client handler
                        pipe.addLast(new CorfuClientHandler());
                    }
                });

        return this;
    }

    public void close() {
       bootstrap = null;
    }

    private Channel connectToProxy() {
        try {
            if (bootstrap == null) {
                throw new RuntimeException("Client not setup");
            }
            return bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public void connect(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.Connect)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void stop(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.Stop)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void stop(boolean shutdown, RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        StopRequest stopRequest = StopRequest.newBuilder()
                .setShutdown(shutdown)
                .build();
        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.Stop)
                .setStopRequest(stopRequest)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void shutdown(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.Shutdown)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void addNode(String endpoint, int retry, Duration timeout, Duration pollPeriod,
                        RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        AddNodeRequest addNodeRequest = AddNodeRequest.newBuilder()
                .setEndpoint(endpoint)
                .setRetry(retry)
                .setTimeout(timeout.toString())
                .setPollPeriod(pollPeriod.toString())
                .build();
        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.AddNode)
                .setAddNodeRequest(addNodeRequest)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void removeNode(String endpoint, int retry, Duration timeout, Duration pollPeriod,
                           RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        RemoveNodeRequest removeNodeRequest = RemoveNodeRequest.newBuilder()
                .setEndpoint(endpoint)
                .setRetry(retry)
                .setTimeout(timeout.toString())
                .setPollPeriod(pollPeriod.toString())
                .build();
        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.RemoveNode)
                .setRemoveNodeRequest(removeNodeRequest)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void forceRemoveNode(String endpoint, int retry, Duration timeout, Duration pollPeriod,
                                 RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ForceRemoveNodeRequest forceRemoveNodeRequest = ForceRemoveNodeRequest.newBuilder()
                .setEndpoint(endpoint)
                .setRetry(retry)
                .setTimeout(timeout.toString())
                .setPollPeriod(pollPeriod.toString())
                .build();
        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.ForceRemoveNode)
                .setForceRemoveNodeRequest(forceRemoveNodeRequest)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void addLayoutServer(String endpoint, RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        AddLayoutServerRequest addLayoutServerRequest = AddLayoutServerRequest.newBuilder()
                .setEndpoint(endpoint)
                .build();
        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.AddLayoutServer)
                .setAddLayoutServerRequest(addLayoutServerRequest)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void getAllServers(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.GetAllServers)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void getAllActiveServers(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.GetAllActiveServers)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }

    public void invalidateLayout(RpcCallback<ProxyResponseMsg> callback) {
        Channel channel = connectToProxy();
        String requestId = generateRequestId();

        ProxyRequestMsg request = ProxyRequestMsg.newBuilder()
                .setClientId(clientId)
                .setMessageId(requestId)
                .setMethodType(MethodType.InvalidateLayout)
                .build();

        callbackMap.put(requestId, callback);
        channel.writeAndFlush(request);
    }
}
