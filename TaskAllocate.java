package com.networkfinalpro;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TaskAllocate {

    private static final int FILE_ANALAYZE_NUM = 3;
    //8080端口负责接收工作站的请求
    public static void main(String[] args) throws IOException {

         Socket accept = null;
         ServerSocket serverSocket = null;
         int count = -1;

        try {
            serverSocket = new ServerSocket(8080);
            while(true)
            {
                count = (count+1)%FILE_ANALAYZE_NUM;
                accept = serverSocket.accept();
                TaskAllocateThread taskAllocateThread = new TaskAllocateThread(accept,count);
                new Thread(taskAllocateThread).start();
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }finally {
            serverSocket.close();
        }

    }
}

class TaskAllocateThread extends Thread{

    private static HashMap<Integer,Integer> portFileNum = new HashMap<>();//端口号 文件数量
    private Socket accept;
    private InputStream inputStream = null;
    private Socket socket;
    private int count;
    static {
        portFileNum.put(0,18088);
        portFileNum.put(1,18089);
        portFileNum.put(2,18090);
    }


    public TaskAllocateThread(Socket socket,int count)
    {
        this.accept = socket;
        this.count = count;
    }


    public void run() {
        int min =0;
        try{
            inputStream = accept.getInputStream();
            byte [] bytes = new byte[64];
            int len = inputStream.read(bytes);
            String string = new String(bytes,0,len);
            //让任务分配服务器休眠一秒以免分析服务器还没准备好接收
            Thread.sleep(1000);
            socket = new Socket(InetAddress.getLocalHost(),portFileNum.get(count));
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            byte[] BfileNum = (string.split("#")[1]+";").getBytes();
            byte[] bport = string.split("#")[2].getBytes();
            //把文件数量和端口号都发送给文件分析服务器
            outputStream.write(BfileNum);
            outputStream.write(bport);
            outputStream.flush();
        }catch (IOException e)
        {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                accept.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
