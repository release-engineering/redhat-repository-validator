package com.redhat.repository.validator.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOutputStream extends OutputStream {

    private final Logger logger;
    private final OutputStream delegate;
    private String buffer;

    public LogOutputStream(String loggerName, OutputStream delegate) {
        this.logger = LoggerFactory.getLogger(loggerName);
        this.delegate = delegate;
        this.buffer = "";
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        writeLog(b);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
        flushLog();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private void writeLog(int b) throws IOException {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        buffer = buffer + new String(bytes);
        if (buffer.endsWith("\n")) {
            buffer = buffer.substring(0, buffer.length() - 1);
            flush();
        }
    }

    private void flushLog() {
        logger.info(buffer);
        buffer = "";
    }

}