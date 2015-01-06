import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN
import static ch.qos.logback.classic.Level.ERROR

appender("STDOUT", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
  }
}
appender("FILE", RollingFileAppender) {
  file = "jel.log"
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd_HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
  }
  rollingPolicy(FixedWindowRollingPolicy) {
    fileNamePattern = "jel.%i.log.zip"
    minIndex = 1
    maxIndex = 10
  }
  triggeringPolicy(SizeBasedTriggeringPolicy) {
    maxFileSize = "2MB"
  }
}
logger("se.liquidbytes.jel", DEBUG)
root(WARN, ["STDOUT", "FILE"])