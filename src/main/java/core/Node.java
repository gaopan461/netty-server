package core;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import core.handler.ServerHandler;

/**
 * 一个节点（进程）
 * @author gaopan
 */
public class Node {
    /** node的id */
    private String id;
    /** node的监听端口 */
    private int port;

    /** 远程节点列表 */
    private Map<String, RemoteNode> remoteNodes = new ConcurrentHashMap<>();
    /** 接收到待处理的请求 */
    private Queue<String> receivedMessages = new ConcurrentLinkedQueue<>();

    public Node(String id, int port) {
        this.id = id;
        this.port = port;
    }

    public void startup() {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        ServerBootstrap b = new ServerBootstrap();
        b.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(256*1024, 512*1024))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 4));
                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new ServerHandler(Node.this));
                    }
                });

            b.bind(port);
    }

    public void pulse() {
        String message;
        while ((message = receivedMessages.poll()) != null) {
            handleMessage(message);
        }

        for (RemoteNode remoteNode : remoteNodes.values()) {
            remoteNode.pulse();
        }
    }

    private void handleMessage(String message) {
        System.out.println(String.format("收到消息：%s，nodeId=%s", message, id));
    }

    public void handlePing(String nodeId, String ip, int port) {
        RemoteNode remoteNode = addRemoteNode(nodeId, ip, port);
        remoteNode.handlePing();
    }

    public RemoteNode addRemoteNode(String nodeId, String ip, int port) {
        RemoteNode newNode = new RemoteNode(this, nodeId, ip, port);
        RemoteNode oldNode = remoteNodes.putIfAbsent(nodeId, newNode);
        if (oldNode == null) {
            newNode.startup();
            return newNode;
        } else {
            return oldNode;
        }
    }

    public void sendMessage(String nodeId, String message) {
        RemoteNode remoteNode = remoteNodes.get(nodeId);
        if (remoteNode != null) {
            remoteNode.sendMessage(message);
        } else {
            System.err.println(String.format("发送消息失败，未知的远程节点，remoteId=%s，message=%s", nodeId, message));
        }
    }

    public void addReceiveMessage(String message) {
        receivedMessages.offer(message);
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }
}
