package com.example.myapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamSource;

import fi.iki.elonen.NanoHTTPD;

class ServerConnection extends NanoHTTPD {

    public ServerConnection(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }
    File file = MainActivity.getOutputMediaFile();

    @Override
    public Response serve(IHTTPSession session) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return newChunkedResponse(Response.Status.OK, "image/jpeg", fis);
    }
}
