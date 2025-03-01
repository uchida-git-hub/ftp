package ftp;

import java.io.BufferedReader;

public class CtrlListen implements Runnable{
    public BufferedReader ctrlInput;
    // コンストラクタ 読み取り先の指定
    public CtrlListen(BufferedReader in){
        ctrlInput = in;
    }

    public void run(){
        while (true){
            try {
                // サーバーからのメッセージをひたすら出力する
                String message = ctrlInput.readLine();
                System.out.println(message);
            }catch (Exception e){
                System.exit(1);
            }
        }
    }

}
