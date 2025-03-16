package ftp_server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ClientHandler implements Runnable{

    // 制御用ソケット
    private Socket ctrlSocket;
    // ユーザー名
    private static final String USERNAME = "username";
    // パスワード
    private static final String PASS = "PASSWORD";
    // ユーザー名の認証
    private boolean usernameAuth = false;
    // ログインの状態
    // true: ログイン中, false: ログアウト中
    private boolean authenticated = false;
    // カレントディレクトリ
    private String currentDir = "ftp_server/rootdir";
    // ルートディレクトリ
    private static final String ROOT_DIR = "ftp_server/rootdir";


    // FtpServerから受け取ったソケットでctrlsocketを初期化する
    public ClientHandler(Socket socket){
        this.ctrlSocket = socket;
    }
    // クライアントのファイル送受信用のポート
    private int clientDataPort;
    // クライアントのIP
    private InetAddress clientDataAddress;

    @Override
    public void run(){
        try(// ftpクライアントからの入力
            BufferedReader ctrlinput = new BufferedReader(new InputStreamReader(ctrlSocket.getInputStream()));
            // ftpクライアントへの出力
            PrintWriter ctrloutput = new PrintWriter(ctrlSocket.getOutputStream(), true);)
        {
            // ftpクライアントへのsocketでの接続成功の通知
            ctrloutput.println("220 Simple FTP Server Ready");
            ctrloutput.flush();
            // 読み込み用
            String line;
            while ((line = ctrlinput.readLine()) != null){
                System.out.println("Client: " + line);
                String[] command = line.split(" ", 2);
                switch (command[0].toUpperCase()) {
                    case "USER":
                        if (command.length > 1) {
                            doUser(command[1], ctrloutput);
                        } else {
                            ctrloutput.println("530 Invalid username.");
                            ctrloutput.flush();
                        }
                        break;
                    case "PASS":
                        if ((command.length) > 1 && usernameAuth) {
                            doPass(command[1], ctrloutput);
                        } else {
                            ctrloutput.println("");
                            ctrloutput.flush();
                        }
                        break;
                    case "CWD":
                        if (command.length > 1 && authenticated) {
                            doCd(command[1], ctrloutput);
                        }
                        break;
                    case "LIST":
                        if (command.length > 1 && authenticated) {
                            doLs(ctrloutput);
                        }
                        break;
                    case "PORT":
                        if (command.length > 1 && authenticated) {
                            doPort(command[1], ctrloutput);
                        }
                        break;
                    case "STOR": // クライアント側からファイルを受信
                        if (command.length > 1 && authenticated){
                            String[] path = command[1].split("\\\\");
                            String filename = path[path.length - 1];
                            System.out.println(filename);
                            doPut(filename, ctrloutput);
                        }
                        break;
                    case "RETR": // クライアント側へファイルを送信
                        if(command.length > 1 && authenticated) {
                            doGet(command[1], ctrloutput);
                        }
                        break;
                    case "QUIT":
                        doQuit(ctrloutput);
                        return;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void doQuit(PrintWriter ctrloutput) throws Exception{
        ctrloutput.println("221 Goodbye.");
        ctrlSocket.close();
    }

    // Userの処理
    public void doUser(String username, PrintWriter ctrloutput) throws Exception{
        if(username.equals(USERNAME)){
            usernameAuth = true;
            ctrloutput.println("331 Username OK, need password.");
        }else {
            ctrloutput.println("530 Invalid username.");

        }
    }

    // Passの処理
    public void doPass(String pass, PrintWriter ctrloutput)throws Exception{
        if(usernameAuth && PASS.equals(pass)){
            authenticated = true;
            ctrloutput.println("230 Login successful.");
        }else{
            // パスワードが違う場合は通信を切断
            // client側は通信が切断されるとプログラムを終了する
            ctrloutput.println("530 Invalid password.");
            ctrlSocket.close();
            
        }
    }

    // CWD
    public void doCd(String command, PrintWriter ctrloutput){
        // 親ディレクトリへ移動
        if(command.startsWith("..")){
            // 現在のディレクトリがROOTディレクトリではない場合
            // rootディレクトリより上の階層は操作させない
            if(!(currentDir.equals(ROOT_DIR))){
                // 親ディレクトリの取得
                Path dir_path = FileSystems.getDefault().getPath(currentDir);
                // カレントディレクトリを親ディレクトリに変更
                currentDir = dir_path.toString();
                // カレントディレクトリ変更後は".."を削除
                command = command.replace("..", "");
            }else{
                // カレントディレクトリより上の階層に進もうとした場合、エラーメッセージを返す
                ctrloutput.println("");
            }
            // commandが"/"のみの場合メッセージを返して終了
            if(command.equals("/")){
                ctrloutput.println();
            }
        }

        // cd後のフォルダの存在確認
        ctrloutput.println("入力されたコマンドは" + command);
        ctrloutput.flush();
        if(!(command.startsWith("/"))) {
    		command = "/" + command;
    	}
        if(Files.exists(Path.of(currentDir + command))){
            // フォルダが存在する場合はカレントディレクトリを移動
            currentDir = currentDir + command;
            ctrloutput.println(currentDir);
        }else {
            // cd後のカレントディレクトリが存在しない場合はエラーメッセージを返す
            ctrloutput.println("このディレクトリは存在しません" + currentDir);
        }
    }

    public void doLs(PrintWriter ctrloutput){
        // 現在のディレクトリを取得
        File file = new File(currentDir);
        // カレントディレクトリ内のファイルおよびディレクトリ
        String[] dirlist = file.list();
        String output = Arrays.toString(dirlist);
        ctrloutput.println(output);
        ctrloutput.flush();
    }

    // クライアントからファイルを受信
    public void doPut(String filename,PrintWriter ctrlOutput) throws Exception{
        byte[] buff = new byte[1024];
        int n;
        String filePath = currentDir + "/" + filename;
        File file = new File(filePath);
        // ファイルはカレントディレクトリへ保存する
        try (Socket dataConnection = new Socket(clientDataAddress, clientDataPort);
             FileOutputStream outFile = new FileOutputStream(file);
             BufferedInputStream dataInput = new BufferedInputStream(dataConnection.getInputStream())){
            while ((n = dataInput.read(buff)) != -1) {
                outFile.write(buff, 0, n);
            }
            ctrlOutput.println("226 Transfer complete.");
            ctrlOutput.flush();
        }catch (IOException e) {
            ctrlOutput.println("550 Failed to save file.");
            ctrlOutput.flush();
        }
    }

    // クライアントにファイルを送信
    public void doGet(String filename ,PrintWriter ctrlOutput) throws Exception{
        byte[] buff = new byte[1024];
        int n;
        // ファイルのパス
        File fileToSend = new File(currentDir, filename);
        // ファイルの存在確認
        if(fileToSend.exists() && fileToSend.isFile()) {
            try (Socket dataConnection = new Socket(clientDataAddress, clientDataPort); // クライアントのサーバーソケットに接続
                 FileInputStream sendFile = new FileInputStream(fileToSend); // 送信ファイルの取得
                 BufferedOutputStream dataOutput = new BufferedOutputStream(dataConnection.getOutputStream())) // ファイル送信用ストリーム
            {
                // ファイルの送信
                while ((n = sendFile.read(buff)) != -1){
                    dataOutput.write(buff, 0, n);
                }
                ctrlOutput.println("226 Transfer complete.");
                ctrlOutput.flush();
            } catch (IOException e) {
                ctrlOutput.println("550 Failed to send file.");
                ctrlOutput.flush();
            }
        }else {
            ctrlOutput.println("550 File not found.");
            ctrlOutput.flush();
        }
    }

    public void doPort(String cmd, PrintWriter ctrloutput) throws Exception{
        // cmdのパース(cmdPars[0] ~ cmdPars[3] -> address, cmdPars[4] ~ cmdPars[5] -> port)
        String[] cmdPars = cmd.split(",");
        if(cmdPars.length == 6){
            // クライアントのIPアドレスを登録
            clientDataAddress = InetAddress.getByName(cmdPars[0] + "." + cmdPars[1] + "." + cmdPars[2] + "." +cmdPars[3]);
            // クライアントのファイル送受信用のポートの登録
            clientDataPort = Integer.parseInt(cmdPars[4])*256 + Integer.parseInt(cmdPars[5]);
            ctrloutput.println("200 PORT command successful.");
            ctrloutput.flush();
        }else {
            ctrloutput.println("501 Syntax error in parameters.");
            ctrloutput.flush();
        }
    }

}
