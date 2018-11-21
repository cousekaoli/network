package com.networkfinalpro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ManagementPlatForm {
    private static int curRow = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9090);
        ManagementThread managementThread = new ManagementThread();
        MyFrame myFrame = managementThread.getMyFrame();
        EventQueue.invokeLater(managementThread);
        while(true)
        {
            Socket accept = serverSocket.accept();
            MessageThread messageThread;
            synchronized (ManagementPlatForm.class){
                messageThread = new MessageThread(accept,myFrame,curRow++);
            }
            new Thread(messageThread).start();
        }

    }
}

class MessageThread implements Runnable{

    private Socket accept;
    private MyFrame myFrame;
    private int curRow;   //在表格中当前连接占第几行

    public MessageThread(Socket socket, MyFrame myFrame, int curRow) {
        this.accept = socket;
        this.myFrame = myFrame;
        this.curRow = curRow;
    }

    @Override
    public void run() {
        try{
            InputStream inputStream = accept.getInputStream();
            while(true)
            {
                byte[] bytes = new byte[100];
                int len = inputStream.read(bytes);
                if(len==-1)//说明对面流已经关闭
                {
                    break;
                }
                String msg = new String(bytes,0,len);
                synchronized (MessageThread.class)
                {
                    if(msg.startsWith("create:"))//第一次会创建一行 端口和对应的信息都会保存
                    {
                        msg = msg.substring(7);
                        String []msgs = msg.split("#");
                        myFrame.addRow(msgs);

                    }else{      //后面直接更改当前行的最后一列信息
                        myFrame.changeRow(msg,curRow,3);
                    }
                }

            }

        }catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}

class ManagementThread implements Runnable{
     private MyFrame myFrame = new MyFrame();
    @Override
    public void run() {
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.setVisible(true);
    }

    public MyFrame getMyFrame() {
        return myFrame;
    }
}

class MyFrame extends JFrame
{
    private static final int DEFAULT_WIDTH = 600;
    private static final int DEAFAULT_HEIGHT = 400;
    private static final int x = 500;
    private static final int y = 200;
    JTable table = null;
    DefaultTableModel defaultModel = null;

    public MyFrame() {
        intiComponent();
    }

    public void changeRow(Object value,int row,int column) {
        defaultModel.setValueAt(value,row,column);
    }

    /**
     * 初始化窗体组件
     */
    private void intiComponent()
    {
        Vector columnNames = createColumnNames();
        Vector data = new Vector();

        // 创建一个默认的表格模型
        defaultModel = new DefaultTableModel(data, columnNames);

        table = new JTable(defaultModel);
        table.setPreferredScrollableViewportSize(new Dimension(600, 200));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        /* 用JScrollPane装载JTable，这样超出范围的列就可以通过滚动条来查看 */
        JScrollPane scroll = new JScrollPane(table);
        add(scroll);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(DEFAULT_WIDTH, DEAFAULT_HEIGHT);
        this.setLocation(x,y);
        this.setTitle("管理平台");
        this.pack();
    }


    public void addRow(Object[] objects){
        defaultModel.addRow(objects);
    }



    private Vector createColumnNames() {
        Vector<String> columnNames = new Vector<String>();
        columnNames.add("名字");
        columnNames.add("IP");
        columnNames.add("端口");
        columnNames.add("工作状态");
        return columnNames;
    }

}

