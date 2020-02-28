package core;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import core.handler.ClientHandler;
import core.handler.MessagePackEncode;
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

    /** netty worker */
    private NioEventLoopGroup worker;

    /** 客户端连接引导器 */
    private Bootstrap bootstrap;
    /** 连接套接字 */
    private Channel channel;
    /** 待发送缓存队列 */
    private Deque<MessagePack> pendingMessages = new ConcurrentLinkedDeque<>();

    /** 最后一次收到连接检查时间 */
    private long lastRecvPingTime;

    public RemoteNode(Node localNode, String id, String ip, int port) {
        this.localNode = localNode;
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public void startup() {
        worker = new NioEventLoopGroup(1);
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
                        pipeline.addLast(new MessagePackEncode());
                        pipeline.addLast(new ClientHandler(RemoteNode.this));
                    }
                });

        reconnect();
    }

    public void shutdown() {
        if (worker != null) {
            worker.shutdownGracefully();
        }
    }

    public void reconnect() {
        Log.core.info("[{} ---> {}]开始连接到服务器，服务器地址={}:{}", localNode.getId(), id, ip, port);

        ChannelFuture future = bootstrap.connect(ip, port);
        future.addListener(f -> {
            if (f.isSuccess()) {
                channel = future.channel();
            } else {
                Log.core.info("[{} ---> {}]连接服务器失败，将在5秒后再次尝试，服务器地址={}:{}，失败原因={}",
                        localNode.getId(), id, ip, port, f.cause().getMessage());
                bootstrap.config().group().schedule(this::reconnect, 5, TimeUnit.SECONDS);
            }
        });
    }

    public void pulse() {

    }

    public void handlePing() {
        lastRecvPingTime = System.currentTimeMillis();
    }

    public void sendMessage(MessagePack messagePack) {
        if (channel == null || !channel.isActive() || !channel.isWritable()) {
            pendingMessages.addLast(messagePack);
        } else {
            channel.writeAndFlush(messagePack);
        }
    }

    public void sendPing() {
        if (channel != null && channel.isActive() || channel.isWritable()) {
            channel.writeAndFlush(localNode.getPingMessage());
        }
    }

    public void flushPendingMessages() {
        MessagePack messagePack = null;
        while ((messagePack = pendingMessages.pollFirst()) != null) {
            if (channel != null && channel.isActive() && channel.isWritable()) {
                channel.writeAndFlush(messagePack);
            } else {
                pendingMessages.addFirst(messagePack);
                break;
            }
        }
    }

    /**
     * 判断是否需要重建RemoteNode<br>
     * 注：如果老节点已经断开，且新节点的ip或端口发生变化，用新节点重建RemoteNode
     * @param newNode
     * @return
     */
    public boolean needRebuild(RemoteNode newNode) {
        if (isActive()) {
            return false;
        } else {
            return !ip.equals(newNode.ip) || port != newNode.port;
        }
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public Node getLocalNode() {
        return localNode;
    }

    public String getId() {
        return id;
    }
}
