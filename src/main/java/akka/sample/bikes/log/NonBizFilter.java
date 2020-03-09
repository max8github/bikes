package akka.sample.bikes.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static ch.qos.logback.core.spi.FilterReply.DENY;

public class NonBizFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return !event.getLoggerName().contains("bikes") ? ACCEPT : DENY;
    }
}