package org.jboss.wolf.validator.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOutputStream extends OutputStream {

    private final Logger logger;
    private String buffer;

    public LogOutputStream(String loggerName) {
        this.logger = LoggerFactory.getLogger(loggerName);
        this.buffer = "";
    }

    @Override
    public void write(int b) throws IOException {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        buffer = buffer + new String(bytes);
        if (buffer.endsWith("\n")) {
            buffer = buffer.substring(0, buffer.length() - 1);
            flush();
        }
    }

    @Override
    public void flush() {
        logger.info(buffer);
        buffer = "";
    }

}