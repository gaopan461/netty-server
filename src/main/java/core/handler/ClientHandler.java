package core.handler;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import core.Log;
import core.RemoteNode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

/**
 * Node通信客户端Handler
 * @author gaopan
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    /** 所属远程node */
    private RemoteNode remoteNode;
    /** 发送Ping的周期任务 */
    private Future<?> pingFuture;

    public ClientHandler(RemoteNode remoteNode) {
        this.remoteNode = remoteNode;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.conn.debug("[{} ---> {}]连接到服务器成功，服务器地址={}", remoteNode.getLocalNode().getId(),
                remoteNode.getId(), ctx.channel().remoteAddress());

        // 开始发送Ping
        startPing(ctx, 0);

        // 刷新一下缓冲队列
        remoteNode.flushPendingMessages();
    }

    private void startPing(ChannelHandlerContext ctx, long initialDelay) {
        // 每3秒发送一次Ping
        pingFuture = ctx.executor().scheduleAtFixedRate(() -> remoteNode.sendPing(), initialDelay, 3, TimeUnit.SECONDS);
        // 如果出异常了，延迟3秒重新开始Ping
        pingFuture.addListener(f -> {
            if (f.cause() != null && !(f.cause() instanceof CancellationException)) {
                startPing(ctx, 3);
            }
        });
    }

    private void stopPing() {
        pingFuture.cancel(true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.conn.debug("[{} <--- {}]服务器断开了连接，服务器地址={}", remoteNode.getLocalNode().getId(),
                remoteNode.getId(), ctx.channel().remoteAddress());

        // 取消ping
        stopPing();

        // 开始重连
        remoteNode.reconnect();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Log.conn.debug("[{} <--- {}]服务器写状态改变，服务器地址={}，writable={}", remoteNode.getLocalNode().getId(),
                remoteNode.getId(), ctx.channel().remoteAddress(), ctx.channel().isWritable());

        // 又可以写了，刷新一下缓冲队列
        if (ctx.channel().isWritable()) {
            remoteNode.flushPendingMessages();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            cause.printStackTrace();
        } finally {
            ReferenceCountUtil.release(cause);
        }
    }
}
