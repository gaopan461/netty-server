package core.handler;

import core.Log;
import core.MessagePack;
import core.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

/**
 * Node通信服务端Handler
 * @author gaopan
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    /** 所属Node */
    private Node node;

    public ServerHandler(Node node) {
        this.node = node;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Log.conn.debug("[{} <--- null]收到客户端连接，客户端地址={}", node.getId(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Log.conn.debug("[{} <--- null]客户端断开了连接，客户端地址={}", node.getId(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        MessagePack messagePack = (MessagePack) msg;
        Log.conn.debug("[{} <--- {}]收到客户端消息，客户端地址={}，消息={}", node.getId(), messagePack.getSender(),
                ctx.channel().remoteAddress(), messagePack.getContext());

        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (messagePack.getType() == MessagePack.TYPE_PING) {
            int port = Integer.parseInt(messagePack.getContext());
            node.handlePing(messagePack.getSender(), inetSocketAddress.getAddress().getHostAddress(), port);
        } else {
            node.addReceiveMessage(messagePack);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            Log.conn.warn("客户端连接出现异常", cause);
        } finally {
            ReferenceCountUtil.release(cause);
        }
    }
}
