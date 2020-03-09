package akka.sample.bikes.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import static ch.qos.logback.core.spi.FilterReply.*;

public class BizDomainFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return event.getLoggerName().contains("bikes") ? ACCEPT : NEUTRAL;
    }
}
