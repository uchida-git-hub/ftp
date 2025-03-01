package ftp_server;

import java.net.*;

public class FtpServer {

    private static final int CTRL_PORT = 21;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(CTRL_PORT)) {
            System.out.println("FTPサーバーがポート" + CTRL_PORT + "で起動しました");
            while (true) {
                // サーバーソケットへの接続を待つ
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}