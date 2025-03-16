package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Ftp3 {
    // 制御用ソケット
    Socket ctrlSocket;
    //出力用制御用ストリーム
    public PrintWriter ctrloutput;
    // 入力制御用ストリーム
    public BufferedReader ctrlinput;
    // ftpのコントロールポート
    final int CTRLPORT = 21;

    // アドレスとポートからソケットを作り制御用ストリームを作成する
    public void openConnection(String host) throws IOException, UnknownHostException{
        ctrlSocket = new Socket(host, CTRLPORT);
        ctrloutput = new PrintWriter(ctrlSocket.getOutputStream());
        ctrlinput = new BufferedReader(new InputStreamReader(ctrlSocket.getInputStream()));
    }

    // 制御用のソケットを終了
    public void closeConnection() throws Exception{
        ctrlSocket.close();
    }

    // menu表示
    public void showMenu(){
        System.out.println(">Command");
        // lsコマンド
        System.out.println("    2 ls");
        // cdコマンド
        System.out.println("    3 cd");
        // getコマンド
        System.out.println("    4 get");
        // putコマンド
        System.out.println("    5 put");
        // 終了
        System.out.println("    9 quit");
    }

    // 利用者のコマンドの入力を読み取り
    public String getCommand(){
        String buf = "";
        BufferedReader lineread = new BufferedReader(new InputStreamReader(System.in));
        while (buf.length() != 1){
            try{
                buf = lineread.readLine();
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
        }
        return buf;
    }

    // サーバーへのログイン
    public void doLogin(){
        String loginName = "";
        String password = "";
        // コンソールから読み込み
        BufferedReader lineread = new BufferedReader(new InputStreamReader(System.in));

        try{
            System.out.println("ログイン名を入力してください");
            loginName = lineread.readLine();
            // ユーザー名の入力
            ctrloutput.println("USER " + loginName);
            ctrloutput.flush();
            // PASSコマンドによる入力
            System.out.println("PASSを入力してください");
            password = lineread.readLine();
            ctrloutput.println("PASS " + password);
            ctrloutput.flush();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void doCd(){
        String dirName = "";
        BufferedReader lineread = new BufferedReader(new InputStreamReader((System.in)));
        try {
            System.out.println("ディレクトリ名を入力してください");
            dirName = lineread.readLine();
            // CWDコマンドをサーバーへ送る
            ctrloutput.println("CWD " + dirName);
            ctrloutput.flush();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void doLs(){
        try{
            int n;
            byte[] buff = new byte[1024];
            // データ用コネクションを作成
            ctrloutput.println("LIST ");
            ctrloutput.flush();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Socket dataConnection(String ctrlcmd){
        String cmd = "PORT ";
        int i;
        Socket dataSocket = null;
        try{
            // 自分のアドレスを求める
            byte[] address = InetAddress.getLocalHost().getAddress();
            System.out.println("address:" + address);
            // serversocketのport0のコンストラクタでは空いているポートが適当に短命ポートとして割り当てられる
            ServerSocket serverDataSocket = new ServerSocket(0, 1);
            for(i = 0; i<4; ++i){
                cmd = cmd + (address[i] & 0xff) + ",";
            }
            cmd = cmd + ((serverDataSocket.getLocalPort()) /256 & 0xff)
                    + ","
                    + (serverDataSocket.getLocalPort() & 0xff);
            ctrloutput.println(cmd);
            ctrloutput.flush();
            // 制御用のコマンドの送信
            ctrloutput.println(ctrlcmd);
            ctrloutput.flush();
            System.out.println("接続待機完了");
            // サーバーからの接続を待つ
            dataSocket = serverDataSocket.accept();
            serverDataSocket.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return dataSocket;
    }

    public void doQuit(){
        try{
            ctrloutput.println("QUIT ");
            ctrloutput.flush();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ファイル取得
    public void doGet(){
        String fileName = "";
        BufferedReader lineread = new BufferedReader(new InputStreamReader(System.in));

        try{
            int n;
            byte[] buff = new byte[1024];
            // サーバー上のファイル名の指定
            System.out.println("ファイル名を入力してください");
            fileName = lineread.readLine();
            // ファイル受信の準備
            FileOutputStream outFile = new FileOutputStream(fileName);
            Socket dataSocket = dataConnection("RETR " + fileName);
            BufferedInputStream dataInput = new BufferedInputStream(dataSocket.getInputStream());
            while ((n = dataInput.read(buff)) > 0){
                outFile.write(buff, 0, n);
            }
            dataSocket.close();
            outFile.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ファイルの送信
    public void doPut(){
        String fileName = "";
        BufferedReader lineread = new BufferedReader(new InputStreamReader(System.in));
        try{
            int n;
            byte[] buff = new byte[1024];
            FileInputStream sendFile = null;
            // ファイル名の指定
            System.out.println("ファイル名を指定してください");
            fileName = lineread.readLine();
            // 送信するファイル
            try {
                System.out.println(fileName);
                sendFile = new FileInputStream(fileName);
            }catch (Exception e){
                System.out.println("ファイルが存在しません");
                return;
            }
            System.out.println("ファイル準備完了");
            // データ送信用のソケットの準備
            Socket dataSocket = dataConnection("STOR " + fileName);
            OutputStream outputStream = dataSocket.getOutputStream();
            // ファイルの送信
            while ((n = sendFile.read(buff)) > 0){
                outputStream.write(buff, 0, n);
            }
            dataSocket.close();
            sendFile.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void doAscii(){
        try{
            // Aモードの指定
            ctrloutput.println("TYPE A");
            ctrloutput.flush();
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
    }

    public void doBinary(){
        try {
            // Iモードの指定
            ctrloutput.println("TYPE I");
            ctrloutput.flush();
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
    }



    // コマンドの入力
    public boolean execCommand(String command){
        boolean cont = true;
        switch (Integer.parseInt(command)) {
            case 2:
                doLs();
                break;
            case 3:
                doCd();
                break;
            case 4:
                doGet();
                break;
            case 5:
                doPut();
                break;
            case 6:
                doAscii();
                break;
            case 7:
                doBinary();
                break;
            case 9:
                doQuit();
                cont = false;
                break;
            default:
                System.out.println("番号を入力してください");
        }
        return cont;
    }

    public void main_proc() throws IOException{
        boolean cont = true;
        try{
            doLogin();
            while(cont){
                // メニューの表示
                showMenu();
                // コマンドを受け取り実行
                cont = execCommand(getCommand());
            }
        }catch (Exception e){
            System.err.print(e);
            System.exit(1);
        }
    }

    // 制御ストリームの受信スレッド
    public void getMsgs(){
        try {
            CtrlListen listener = new CtrlListen(ctrlinput);
            Thread listenerThread = new Thread(listener);
            listenerThread.start();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args){
        try {
            Ftp3 f = null;
            f = new Ftp3();
            f.openConnection(args[0]);
            f.getMsgs();
            f.main_proc();
            f.closeConnection();
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

}
