package com.networkfinalpro;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

//工作站 发送请求给任务分配服务器 任务分配服务器获得ip和端口
public class Workstation {

    private static int[] ports = {18081,18082,18083,18084,18085,18086,18087};

    public static void main(String[] args) throws IOException {

        for(int i=0;i<ports.length;i++)
        {
            new Thread(new WorkStationThread(ports[i],i+1)).start();

        }
    }
}

class WorkStationThread extends Thread{

    private int port;
    private int count;//是第几个线程
    private ServerSocket serverSocket;
    private Socket socket;
    private Socket ManagementSocket;
    private  OutputStream managementOutputStream;
    private static final int TASK_ALLOCATION_PORT = 8080;   //任务分配服务器端口
    private int fileNum ;    //传送的文件个数 总的文件个数 需要自己去获取
    private int cur = 0;//当前已经传输的文件个数
    private Socket accept;
    private boolean normal = false;

    @Override
    public void run() {

        try{
            serverSocket = new ServerSocket(port);//给管理平台更新信息 发送UDP包过去不行 可能接收不到

            //向管理平台发送信息
            ManagementSocket = new Socket(InetAddress.getLocalHost(),9090);
            managementOutputStream = ManagementSocket.getOutputStream();
            byte [] createMsg = ("create:工作站#"+InetAddress.getLocalHost().getHostAddress()+"#"+port+"#没有传送文件").getBytes();
            managementOutputStream.write(createMsg);
            managementOutputStream.flush();

            socket = new Socket(InetAddress.getLocalHost(),TASK_ALLOCATION_PORT);
            OutputStream requestStream = socket.getOutputStream();
            File directory = new File("F:\\networkTest\\workstationfinal\\workstation"+count);
            File[] files = directory.listFiles();
            fileNum = files.length;
            byte[] bytes = ("request#"+fileNum+"#"+port).getBytes();
            requestStream.write(bytes);

            //等待分析服务器连接
            accept = serverSocket.accept();
            OutputStreamWriter outPutStream = new OutputStreamWriter(accept.getOutputStream(), "GBK");//开始准备传输文件 给管理平台更新信息
            InputStream inputStream = accept.getInputStream();

            String filename;
            String string;
            byte [] newMsg = "正在传送文件".getBytes();
            managementOutputStream.write(newMsg);
            managementOutputStream.flush();
            while(cur<fileNum)
            {

                StringBuilder totalContent = new StringBuilder();
                string =new String(Files.readAllBytes(files[cur].toPath()),"GBK");
                filename = files[cur].getName();
                cur++;
                totalContent.append(string.length()+"#"+filename+string);
                outPutStream.write(totalContent.toString());
                outPutStream.flush();
                byte []response = new byte[100];
                int len = inputStream.read(response, 0, response.length);
               System.out.println("分析服务器返回内容:"+new String(response,0,len));
            }


            newMsg = "传送文件完毕".getBytes();
            managementOutputStream.write(newMsg);
            managementOutputStream.flush();
            normal = true;
        }catch (IOException e)
        {
            e.printStackTrace();
        }  finally {
            try {
                if(normal)
                {
                    managementOutputStream.write("正常关机".getBytes());
                    managementOutputStream.flush();
                }else{
                    managementOutputStream.write("宕机".getBytes());
                    managementOutputStream.flush();
                }
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        socket.close();
        accept.close();
        ManagementSocket.close();
        serverSocket.close();
    }

    public WorkStationThread(int port,int count) {
        this.port = port;
        this.count = count;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
