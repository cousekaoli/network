package com.networkfinalpro;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class FileAnalyze {

    private static int ports []= {18088,18089,18090};//监听的端口

    public static void main(String[] args) throws IOException, InterruptedException {

        for(int i=0;i<ports.length;i++)
        {
            new FileReceiveThread(ports[i]).start(); //把接收到的文件共享给另一个线程进行分析
        }
    }

}

class FileAnalyzeThread extends Thread{
    private CircleQueue circleQueue;
    private int objPort;
    private OutputStream managementOutputStream;
    private boolean normal = false;

    public FileAnalyzeThread(CircleQueue circleQueue, int objPort, OutputStream managementOutputStream) {
        this.circleQueue = circleQueue;
        this.objPort = objPort;
        this.managementOutputStream = managementOutputStream;
    }

    @Override
    public void run() {
        try{
            while(!circleQueue.isEmpty())
            {
                StringBuilder eachContent = new StringBuilder();
                String fileS = circleQueue.poll();
                int index = fileS.indexOf("txt"); //这两行获取文件名字
                if(index==-1)
                {
                    continue;
                }
                String filename = fileS.substring(0,index+3);
                String subFile = fileS.substring(index+3);
                eachContent.append(filename+"  ").append(InetAddress.getLocalHost().getHostAddress()+"  "+objPort+"  "+new Date());
                byte[] conentB = new byte[8];
                byte[] original = subFile.getBytes();
                for(int j=0;j<8;j++)
                {
                    conentB[j] = original[j];
                }
                String eightB = new String(conentB,0,conentB.length);
                eachContent.append("  "+eightB+"  "+original.length+"b\n");
                synchronized (FileAnalyze.class)
                //在打开文件输出流传输信息的时候加上锁
                {
                    OutputStreamWriter fileOutputStream = new OutputStreamWriter(new FileOutputStream("F:\\networkTest\\fileanalyze\\FileResult.log",true)) ;//追加
                    fileOutputStream.write(eachContent.toString());
                    fileOutputStream.flush();
                }
            }
            normal = true;//正常运行到最后一行
          //  Thread.sleep(1000);

        }catch (IOException e)
        {
            e.printStackTrace();
        }finally {
            try {
            if(normal==true)
            {
                managementOutputStream.write("没有文件被分析".getBytes());

            }else{
                managementOutputStream.write("宕机".getBytes());
                managementOutputStream.close();
            }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }
}

class FileReceiveThread extends Thread{

    private int port;
    private ServerSocket serverSocket;
    private Socket accept;
    private Socket ManagementSocket;
    private  OutputStream managementOutputStream;
    private CircleQueue circleQueue = new CircleQueue();

    public FileReceiveThread(int port)
    {

        this.port = port;
    }

    @Override
    public void run() {
        try{
            serverSocket = new ServerSocket(port);//给管理平台更新信息
            ManagementSocket = new Socket(InetAddress.getLocalHost(),9090);
            System.out.println(ManagementSocket);
            managementOutputStream = ManagementSocket.getOutputStream();
            byte [] createMsg = ("create:文件分析服务器#"+InetAddress.getLocalHost().getHostAddress()+"#"+port+"#准备接受文件").getBytes();
            managementOutputStream.write(createMsg);
            managementOutputStream.flush();
            while(true)
            {

                accept = serverSocket.accept();

                managementOutputStream.write("接收文件中".getBytes());
                managementOutputStream.flush();

                InputStream getNumAndPortStream = accept.getInputStream();
                byte[] portAndNumInput = new byte[64];
                int len = getNumAndPortStream.read(portAndNumInput);
                String objectPortAndNum = new String(portAndNumInput,0,len);
                String objectPort = objectPortAndNum.split(";")[1];
                String fileNum = objectPortAndNum.split(";")[0];
                Socket socket = new Socket(InetAddress.getLocalHost(),Integer.parseInt(objectPort));
                System.out.println(port+"端口分析服务器已与"+objectPort+"端口工作站连接");
                InputStream fileGetStream = socket.getInputStream();
                OutputStream fileOutPutStream = socket.getOutputStream();
                write(fileGetStream,fileOutPutStream,Integer.parseInt(fileNum),circleQueue);
                managementOutputStream.write("文件分析中".getBytes());
                managementOutputStream.flush();
                new FileAnalyzeThread(circleQueue,Integer.parseInt(objectPort),managementOutputStream).start();//开启一个线程进行分析

            }
        }
        catch (IOException e)
        {
            try {
                managementOutputStream.write("宕机".getBytes());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } catch (InterruptedException e) {
            try {
                managementOutputStream.write("宕机".getBytes());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private  void write(InputStream fileGetStream,OutputStream fileOutPutStream ,int fileNum, CircleQueue circleQueue) throws IOException, InterruptedException {

        for(int i=0;i<fileNum;i++)
        {
            byte [] bytes = new byte[1024];
            int len = 0;
            StringBuilder content = new StringBuilder();
            boolean b = false;
            int length = 0;
            int index = 0;
            int totalLength =0;
            int indexTXT = 0;
            String cur;
            String filename ="";
            while((len = fileGetStream.read(bytes))!=-1)
           //-1只有对面socket关闭或者流关闭才结束 所以加一个文件长度信息 当读到文件长度大小的字节后退出循环
            {
                cur = new String(bytes,0,len);
                content.append(cur);
                if(b==false)//第一次读取一定能读到#
                {
                    index=content.indexOf("#");
                    if(index!=-1)
                    {
                        b = true;
                        length = Integer.parseInt(content.substring(0,index));
                        indexTXT = content.indexOf("txt");
                        filename = cur.substring(index+1,indexTXT+3);
                        totalLength +=content.substring(indexTXT+3).length();
                    }
                }else{
                    totalLength +=cur.length();
                }
                if(totalLength==length)
                {
                    break;
                }
            }
            String filecontent = content.substring(indexTXT+3);
            String response = "pre8:"+filecontent.toString().substring(0,8)+" length:"+filecontent.toString().length();
            fileOutPutStream.write(response.getBytes());
            fileOutPutStream.flush();
            if(!circleQueue.isFull())
            {
                  circleQueue.add(filename+filecontent);
            }
        }
        fileGetStream.close();
        fileOutPutStream.close();
    }




    public void close()
    {
        try{
            accept.close();
            serverSocket.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}

class CircleQueue{
    private int front;
    private int tail;
    private static final int LENGTH=300;
    private String []files;

    public void add(String string){
        files[tail] = string;
        tail = (tail+1)%LENGTH;
    }

    public String poll(){
        String result = files[front];
        front = (front+1)%LENGTH;
        return result;
    }


    public CircleQueue() {
        front = 0;
        tail = 0;
        files = new String[LENGTH];
    }

    public boolean isEmpty(){
        return front==tail;
    }

    public boolean isFull(){
        if((tail+1)%LENGTH==front)
        {
            return true;
        }
        return false;
    }

}
