package org.jenkinsci.plugins.leiningen;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author Tom Denley
 * @see hudson.tasks._ant.AntConsoleAnnotator
 */
public class LeinConsoleAnnotator extends LineTransformationOutputStream {
    private final OutputStream out;
    private final Charset charset;

    public LeinConsoleAnnotator(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();
        line = trimEOL(line);
        out.write(b, 0, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

}
