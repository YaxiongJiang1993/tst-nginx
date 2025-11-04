package com.tst.nginx.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ServerSocketTest {

    NginxConfig nginxConfig = new NginxConfig();

    public static void main(String[] args) {
        ServerSocketTest test = new ServerSocketTest();
        test.testGetResource();
        /*test.init();
        test.start();*/
    }

    private void init() {
        nginxConfig.setHttpServerRoot("file");
        nginxConfig.setHttpServerListen(8080);
    }

    private void testGetResource() {
        System.out.println(ServerSocketTest.class.getResource("index.html"));
        System.out.println(ServerSocketTest.class.getResource("/index.html"));
        System.out.println();
        System.out.println(ServerSocketTest.class.getClassLoader().getResource("index.html"));
        System.out.println(ServerSocketTest.class.getClassLoader().getResource("/index.html"));

    }

    public void start() {

        try {
            // 服务器监听的端口号
            int port = nginxConfig.getHttpServerListen();
            ServerSocket serverSocket = new ServerSocket(port);
            log.info("[Nginx4j] listen on port={}", port);

            while (true) {
                Socket socket = serverSocket.accept();
                log.info("[Nginx4j] Accepted connection from address={}", socket.getRemoteSocketAddress());
                handleClient(socket);
            }
        } catch (Exception e) {
            log.info("[Nginx4j] meet ex", e);

            throw new RuntimeException(e);
        }
    }

    private void handleClient(Socket socket) {
        try {
            // 基本信息
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String header = reader.readLine();
            String[] parts = header.split(" ");
            String method = parts[0];
            String path = parts[1];
            String protocol = parts[2];

            // 根路径
            final String basicPath = nginxConfig.getHttpServerRoot();
            // 只处理GET请求
            if ("GET".equalsIgnoreCase(method)) {
                //root path
                log.info("[Nginx4j] current path={}, match index path", path);
                if (StringUtils.isEmpty(path) || "/".equals(path)) {
                    byte[] fileContent = tryGetIndexContent();
                    sendResponse(socket, 200, "OK", fileContent);
                    return;
                }

                // other
                String pathName = basicPath + path;
                String realPath = ServerSocketTest.class.getClassLoader().getResource(pathName).getFile();
                File file = new File(realPath);
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    sendResponse(socket, 200, "OK", fileContent);
                } else {
                    sendResponse(socket, 404, "Not Found", "File not found.".getBytes());
                }
            } else {
                sendResponse(socket, 405, "Method Not Allowed", "Method not allowed.".getBytes());
            }
        } catch (Exception e) {
            // 异常处理...
            log.error("handleClient exception msg: {}", e.getMessage(), e);
        }
    }

    private void sendResponse(Socket socket, int statusCode, String statusMessage, byte[] content) throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        // 发送HTTP响应头
        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: " + content.length);
        writer.println("Connection: close");
        writer.println();

        // 发送HTTP响应体
        output.write(content);
        output.flush();
    }

    private byte[] tryGetIndexContent() {
        String path = ServerSocketTest.class.getClassLoader().getResource("index.html").getFile();
        byte[] fileContent = new byte[0];
        try {
            fileContent = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileContent;
    }
}
