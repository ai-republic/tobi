package com.airepublic.microprofile.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetScanner {
    static class Worker implements Runnable {
        private final int j;


        public Worker(final int j) {
            this.j = j;
        }


        @Override
        public void run() {
            System.out.println("Scanning: 192.168." + j + ".[0-255]");

            for (byte i = 0; i < 256; i++) {
                try {
                    final InetAddress addr = InetAddress.getByName("192.168." + j + "." + i);

                    if (addr.isReachable(1000)) {
                        System.out.println("Found response under: " + addr.getHostAddress() + " -> " + addr.getHostName());
                    }

                } catch (final UnknownHostException e) {
                } catch (final IOException e) {
                }
            }
        }
    }


    public static void main(final String[] args) {
        final ExecutorService executor = Executors.newFixedThreadPool(256);

        for (int j = 0; j < 256; j++) {
            executor.execute(new Worker(j));
        }
    }
}
