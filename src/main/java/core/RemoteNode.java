package core;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringEncoder;
import core.handler.ClientHandler;

/**
 * 远程Node
 * @author gaopan
 */
public class RemoteNode {
    /** 本地node */
    private Node localNode;

    /** 远程node的id */
    private String id;
    /** 远程node的地址 */
    private String ip;
    /** 远程node的端口 */
    private int port;

    /** 客户端连接引导器 */
    private Bootstrap bootstrap;
    /** 连接套接字 */
    private Channel channel;
    /** 待发送缓存队列 */
    private Deque<String> pendingMessages = new ConcurrentLinkedDeque<>();

    /** 最后一次收到连接检查时间 */
    private long lastRecvPingTime;

    public RemoteNode(Node localNode, String id, String ip, int port) {
        this.localNode = localNode;
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public void startup() {
        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(256*1024, 512*1024))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LengthFieldPrepender(4, true));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new ClientHandler(RemoteNode.this));
                    }
                });

        reconnect();
    }

    public void reconnect() {
        System.out.println(String.format("开始连接，remoteId=%s，remoteAddr=%s:%d", id, ip, port));

        ChannelFuture future = bootstrap.connect(ip, port);
        future.addListener(f -> {
            if (f.isSuccess()) {
                channel = future.channel();
            } else {
                // 连接失败，5秒后再次尝试
                System.out.println(String.format("连接失败，将在5秒后再次尝试，remoteId=%s，remoteAddr=%s:%d", id, ip, port));
                bootstrap.config().group().schedule(this::reconnect, 5, TimeUnit.SECONDS);
            }
        });
    }

    public void pulse() {

    }

    public void handlePing() {
        lastRecvPingTime = System.currentTimeMillis();
    }

    public void sendMessage(String message) {
        message = String.format("%s:%s", localNode.getId(), message);
        if (channel == null || !channel.isActive() || !channel.isWritable()) {
            pendingMessages.addLast(message);
        } else {
            channel.writeAndFlush(message);
        }
    }

    public void sendPing() {
        if (channel != null && channel.isActive() || channel.isWritable()) {
            channel.writeAndFlush(String.format("%s:Ping:%d", localNode.getId(), localNode.getPort()));
        }
    }

    public void flushPendingMessages() {
        String message = null;
        while ((message = pendingMessages.pollFirst()) != null) {
            if (channel != null && channel.isActive() && channel.isWritable()) {
                channel.writeAndFlush(message);
            } else {
                pendingMessages.addFirst(message);
                break;
            }
        }
    }

    public String getId() {
        return id;
    }
}
