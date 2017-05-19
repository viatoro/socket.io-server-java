/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.sample.tomcat;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class ChatServer {

    private static WebResourceSet[] getRoots(WebResourceRoot resources) throws Exception {
        URL root = ChatServer.class.getClassLoader().getResource("");
        if (root == null) {
            root = ChatServer.class.getClassLoader().getResource("chat.html");
            if (root == null) {
                throw new IllegalArgumentException("Cannot identify static resources root");
            }
        }
        if ("jar".equals(root.getProtocol())) {
            String path = URI.create(root.getPath().substring(0, root.getPath().lastIndexOf("!/"))).getPath();
            return new WebResourceSet[]{
                    new JarResourceSet(resources, "/WEB-INF/classes", path, "/"),
                    new JarResourceSet(resources, "/", path, "/")};
        } else if ("file".equals(root.getProtocol())) {
            return new WebResourceSet[]{
                    new DirResourceSet(resources, "/WEB-INF/classes", root.getPath(), "/"),
                    new DirResourceSet(resources, "/", root.getPath(), "/")
            };
        } else {
            throw new IllegalArgumentException("Cannot identify static resources root");
        }
    }

    private static File createTempDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }

    public static void main(String args[]) throws Exception {

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);


        File workdir = createTempDirectory();
        File context = new File(workdir, "context.xml");
        new FileWriter(context).write("<Context></Context>");
        context.deleteOnExit();
        workdir.deleteOnExit();

        StandardContext ctx = (StandardContext) tomcat.addWebapp("/", workdir.toString());
        WebResourceRoot resources = new StandardRoot(ctx);
        for (WebResourceSet set : getRoots(resources)) {
            resources.addPreResources(set);
        }
        ctx.setResources(resources);

        tomcat.start();
        tomcat.getServer().await();
    }
}
