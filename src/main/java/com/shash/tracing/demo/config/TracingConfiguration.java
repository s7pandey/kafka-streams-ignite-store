package com.shash.tracing.demo.config;

import org.springframework.cloud.sleuth.instrument.web.mvc.SpanCustomizingAsyncHandlerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import brave.Tracing;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.handler.FinishedSpanHandler;
import brave.handler.MutableSpan;
import brave.kafka.clients.KafkaTracing;
import brave.kafka.streams.KafkaStreamsTracing;
import brave.messaging.MessagingTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;


public class TracingConfiguration {
    final KafkaTracing kafkaTracing;
    final KafkaStreamsTracing kafkaStreamsTracing;

    static TracingConfiguration create(String defaultServiceName) {
        Tracing tracing = TracingFactory.create(defaultServiceName);
        return new TracingConfiguration(MessagingTracing.create(tracing));
    }

    TracingConfiguration(MessagingTracing messagingTracing) {
        this.kafkaTracing = KafkaTracing.create(messagingTracing);
        this.kafkaStreamsTracing = KafkaStreamsTracing.create(kafkaTracing);
    }

    static final class TracingFactory {
        static final BaggageField USER_NAME = BaggageField.create("userName");

        /**
         * Controls aspects of tracing such as the service name that shows up in the UI
         */
        static Tracing create(String serviceName) {
            return Tracing.newBuilder().localServiceName(serviceName)
                    .supportsJoin(Boolean.parseBoolean(System.getProperty("brave.supportsJoin", "true")))
                    .propagationFactory(propagationFactory())
                    .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().build())
                    .spanReporter(AsyncReporter.create(sender())).build();
        }

        /**
         * Configures propagation for {@link #USER_NAME}, using the remote header
         * "user_name"
         */
        static Propagation.Factory propagationFactory() {
            return BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY).add(
                    BaggagePropagationConfig.SingleBaggageField.newBuilder(USER_NAME).addKeyName("user_name").build())
                    .build();
        }

        /** Configuration for how to send spans to Zipkin */
        static Sender sender() {
            return URLConnectionSender
                    .create(System.getProperty("zipkin.baseUrl", "http://127.0.0.1:9411") + "/api/v2/spans");
        }

        /** Configuration for how to buffer spans into messages for Zipkin */
        static FinishedSpanHandler spanHandler(Sender sender) {
            final FinishedSpanHandler spanHandler = new FinishedSpanHandler() {

                @Override
                public boolean handle(TraceContext context, MutableSpan span) {
                    // TODO Auto-generated method stub
                    return false;
                }
          
      };

    //   Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    //     spanHandler.close(); // Make sure spans are reported on shutdown
    //     try {
    //       sender.close(); // Release any network resources used to send spans
    //     } catch (IOException e) {
    //       System.out.println("error closing trace sender: " + e.getMessage());
    //     }
    //   }));

      return spanHandler;
    }

    private TracingFactory() {
    }
  }
}
