/*
 * Copyright 2013-2017 Spotify AB. All rights reserved.
 *
 * The contents of this file are licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spotify.ffwd.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.ffwd.model.Batch;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class HttpDecoder extends MessageToMessageDecoder<FullHttpRequest> {
    public static final int MAX_FRAME_SIZE = 0xffffff;

    @Inject
    @Named("application/json")
    private ObjectMapper mapper;

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest in, List<Object> out)
        throws Exception {

        final Batch batch;
        try (final InputStream inputStream = new ByteBufInputStream(in.content())) {
            batch = mapper.readValue(inputStream, Batch.class);
        } catch(final IOException e) {
            throw new BadRequest();
        }

        log.info("HttpRequest: {}", in.content());
        log.info("Batch: {}", batch);

        out.add(batch);
        
        ctx.channel()
           .writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
           .addListener(new ChannelFutureListener() {
               @Override
               public void operationComplete(final ChannelFuture future) throws Exception {
                   future.channel()
                         .close();
               }
           });
    }
}
