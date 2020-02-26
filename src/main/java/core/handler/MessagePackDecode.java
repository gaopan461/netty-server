package core.handler;

import java.util.List;

import core.MessagePack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * @author gaopan
 */
public class MessagePackDecode extends MessageToMessageDecoder<String> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, String msg, List<Object> list) throws Exception {
        list.add(MessagePack.decode(msg));
    }
}
