/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cat.champel.learning.camel;

import java.util.HashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.impl.DefaultCamelContext;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

public final class FileToStreamExample {
	
	@SuppressWarnings("serial")
	static class Event extends HashMap<String,Object> {};

    public static void main(String[] args) throws Exception {

        try (CamelContext context = new DefaultCamelContext()) {
        	CamelReactiveStreamsService camel = CamelReactiveStreams.get(context);
        	Publisher<Event> smallEvents = camel.fromStream("event", Event.class);
        	Flowable.fromPublisher(smallEvents)
	            .doOnNext(value -> System.out.println("Event: " + value))
	            .subscribe();            
        	Publisher<String> bigEvents= camel.fromStream("archive", String.class);
        	Flowable.fromPublisher(bigEvents)
	            .doOnNext(value -> System.out.println("Archive: " + value))
	            .subscribe();            


            context.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("direct:test").to("file:test/ready");
                    from("file:test/ready?move=../done")
                    	.split(body().tokenize("\n"))
                    	.unmarshal().json(Event.class)
                    	.to("reactive-streams:event");
                    from("file:test/done?move=../store")
                    	.transform().simple("${header.CamelFileName}")
                    	.to("reactive-streams:archive");
                }
            });
            
            try (ProducerTemplate template = context.createProducerTemplate()) {
                context.start();
                for (int i = 0; i < 3; i++) {
                    template.sendBody("direct:test", "{\"a\":" + i +"}\n{\"b\":" + i + "}");
                }
            }
            Thread.sleep(3000);
            context.stop();
        }
    }
}
