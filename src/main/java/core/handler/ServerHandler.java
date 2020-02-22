package core.handler;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import core.Node;

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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(String.format("连接建立，remoteAddr=%s", ctx.channel().remoteAddress()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(String.format("连接端口，remoteAddr=%s", ctx.channel().remoteAddress()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(String.format("收到消息：%s，remoteAddr=%s", msg, ctx.channel().remoteAddress()));
        String[] ss = ((String)msg).split(":");
        String nodeId = ss[0];
        String context = ss[1];

        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (context.equals("Ping")) {
            node.handlePing(nodeId, inetSocketAddress.getAddress().getHostAddress(), Integer.parseInt(ss[2]));
        } else {
            node.addReceiveMessage((String)msg);
        }
    }
}
