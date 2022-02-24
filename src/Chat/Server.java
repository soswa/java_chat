package Chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/*
 * ä�� ���α׷� [����]
 * 1. Ŭ���̾�Ʈ�κ��� ��ȭ ������ ���� �޾Ƽ� ��ü Ŭ���̾�Ʈ�鿡�� �����ش�.
 * 2. �ߺ��Ǵ� �г����� �ִ��� �˻��ؼ� �α����� �����Ѵ�.
 * 3. ���� ���� �г��� ����� ��ü Ŭ���̾�Ʈ�鿡�� �����ش�.
 * 4. ��ü ä�ð� �ӼӸ� ä���� �����ؼ� �����ش�.
 */

class ClientManger extends Thread {
   Socket socket;
   String name;
   static ArrayList<ClientManger> clientMangers = new ArrayList<>();

   public ClientManger(Socket socket) {
      this.socket = socket;
   }

   Boolean isDuplicatedName(String name) {
      for (ClientManger clientManger : clientMangers) {
         if(clientManger.name.equals(name)) {
            return true;
         }
      }
      return false;
   }

   String getStrUserList() {
      String strUserList = "";
      for (ClientManger clientManger : clientMangers) {
         strUserList += clientManger.name + " ";
      }
      return strUserList;
   }

   void removeClient(String name) {
      for (int i = 0; i < clientMangers.size(); i++) {
         if(clientMangers.get(i).name.equals(name)) {
            clientMangers.remove(i);
            break;
         }
      }
   }

   @Override
   public void run() {

      try {

         // �г��� �ޱ�
         InputStream is = socket.getInputStream();
         byte[] data = new byte[1024];
         int size = is.read(data);
         if(size == -1) {
            return;
         }

         String strData = new String(data, 0, size);

         // �г��� �ߺ� �˻�
         if(isDuplicatedName(strData)) {
            System.out.println("�̸� �ߺ� �Դϴ�.");
            OutputStream os = socket.getOutputStream();
            data = "�̸��ߺ�".getBytes();
            os.write(data);
            return;
         }
         else {
            System.out.println("�̸� �ߺ� �ƴմϴ�.");
            OutputStream os = socket.getOutputStream();
            data = "�̸��ߺ��ƴ�".getBytes();
            os.write(data);
         }

         name = strData;
         System.out.println("���� �̸� : " + name);

         // �̸����� ������ �Ǹ�, �־��ش�.
         clientMangers.add(this);

         // ��� Ŭ���̾�Ʈ���� ������ �˸���.
         sendMessage(null, "�ο����");

         // Ŭ���̾�Ʈ�κ��� ��ȭ�� �ް� ������.
         while(true) {

            System.out.println("Ŭ���̾�Ʈ ��ȭ ��� ��...");
            data = new byte[1024];
            size = is.read(data);
            if(size == -1) {
               System.out.println(name + " Ŭ���̾�Ʈ�� ������ ������.");
               removeClient(name);
               sendMessage(name, "�α׾ƿ�");
               sendMessage(null, "�ο����");
               break;
            }

            // ���� ��ȭ�� ��� Ŭ���̾�Ʈ�鿡�� �����Ѵ�.
            strData = new String(data, 0, size);
            sendMessage(strData, "��ȭ����");
         }

      } catch (IOException e) { e.printStackTrace(); }

   }

   /*
    * type : "��ȭ����", "�ο����"
    */
   int sendMessage(String message, String type) {
      String targetName = "";

      //�޼��� ������ �÷��� �߰�
      switch(type) {
      case "�ο����" : message = type + "!" + getStrUserList(); break;
      case "�α׾ƿ�" : message = type + "!" + message + " ���� �������ϴ�."; break;
      case "��ȭ����" : 
         // ��ȭ ���� �Ǻ�
         String[] strArrMessage = message.split(" ", 3);
         // �ӼӸ�
         if(strArrMessage[0].equals("/r")) {
            targetName = strArrMessage[1];
            System.out.println("���̵� : " + targetName);
            message = type + "!" + name + " : " + strArrMessage[2]; 
            System.out.println("���� : " + message);
            break;
         } 
         else {
            message = type + "!" + name + " : " + message; break;
         }
      }

      try {
         //** �ڵ� ����ϰ� ������ ��
         if(!targetName.isEmpty()) {
            for (ClientManger clientManger : clientMangers) {

               if(clientManger.name.equals(targetName) || clientManger.name.equals(this.name)) {
                  System.out.println("�ӼӸ� ���");
                  OutputStream os = clientManger.socket.getOutputStream();
                  byte[] data = message.getBytes();
                  os.write(data);
               }

            }
         }
         else {
            for (ClientManger clientManger : clientMangers) {
                  OutputStream os = clientManger.socket.getOutputStream();
                  byte[] data = message.getBytes();
                  os.write(data);
            }
         }


      } catch (IOException e) { 
         e.printStackTrace();
         return -1;
      }

      return 1;

   }

   String byte2String(byte[] data) {
      return new String(data, 0, data.length);
   }

   byte[] String2byte(String str) {
      return str.getBytes();
   }
}

class ServerManager {
   static String serverIP = "localhost";
   static int port = 5001;
   static ServerSocket serverSocket;

   public ServerManager(String serverIP, int port) {
      this.serverIP = serverIP;
      this.port = port;
   }

   void openServer() {
      System.out.println("���� ����");

      new Thread() {
         @Override
         public void run() {

            try {

               serverSocket = new ServerSocket();
               if(serverSocket == null) {
                  System.out.println("���� ���� ���� : ���� ������ ������ �� �����ϴ�.");
                  return;
               }
               System.out.println("���� ���� �Ϸ�");

               serverSocket.bind(new InetSocketAddress(serverIP, port));

               // Ŭ���̾�Ʈ ������ ������ �޴´�.
               while(true) {
                  System.out.println("Ŭ���̾�Ʈ ���� ���");
                  Socket socket = serverSocket.accept();
                  System.out.println("Ŭ���̾�Ʈ ����");

                  ClientManger clientManger = new ClientManger(socket);
                  clientManger.start();
               }

            } catch(SocketException e) {
               System.out.println("���� ���� ������.");
            }  catch (IOException e) { e.printStackTrace(); }

         }
      }.start();

   }

   void closeServer() {
      try {
         ClientManger.clientMangers.clear();
         serverSocket.close();
      } catch (IOException e) { e.printStackTrace(); }
   }
}

class ServerPage {

   Stage stage;
   ServerManager serverManager;

   ServerPage(Stage stage) {
      this.stage = stage;

      createMainPage();

      serverManager = new ServerManager(ServerManager.serverIP, ServerManager.port);
   }

   // UI ����
   void createMainPage() {

      VBox root = new VBox();
      root.setPrefSize(400, 400);

      // --- UI ��Ʈ�� ---

      Button btnOpenServer = new Button("���� ����");
      btnOpenServer.setOnAction((arg0) -> {
         // ���� ����
         serverManager.openServer();
      });

      Button btnCloseServer = new Button("���� �ݱ�");
      btnCloseServer.setOnAction((arg0) -> {
         // ���� �ݱ�
         // serverManager.stop();
         serverManager.closeServer();
      });

      root.getChildren().addAll(btnOpenServer, btnCloseServer);

      // --- UI ��Ʈ�� ---

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.setTitle("ä�� Ŭ���̾�Ʈ");
      stage.setResizable(false);
      stage.setOnCloseRequest(event -> { 
         //windowContainer.showClosingPopup("Exit"); 
         terminate(); 
      });
      stage.show();

   }

   void terminate() {
      System.exit(0);
   }
}

public class Server extends Application {

   @Override
   public void start(Stage stage) throws Exception {
      new ServerPage(stage);
   }

   public static void main(String[] args) {
      System.out.println("���� ���α׷� ����");
      launch();
   }

}