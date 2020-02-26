package core.handler;

import java.util.List;

import core.MessagePack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * @author gaopan
 */
public class MessagePackEncode extends MessageToMessageEncoder<MessagePack> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MessagePack messagePack, List<Object> list) throws Exception {
        list.add(messagePack.encode());
    }
}
