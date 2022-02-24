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
 * 채팅 프로그램 [서버]
 * 1. 클라이언트로부터 대화 내용을 전달 받아서 전체 클라이언트들에게 보내준다.
 * 2. 중복되는 닉네임이 있는지 검사해서 로그인을 차단한다.
 * 3. 접속 중인 닉네임 목록을 전체 클라이언트들에게 보내준다.
 * 4. 전체 채팅과 귓속말 채팅을 구분해서 보내준다.
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

         // 닉네임 받기
         InputStream is = socket.getInputStream();
         byte[] data = new byte[1024];
         int size = is.read(data);
         if(size == -1) {
            return;
         }

         String strData = new String(data, 0, size);

         // 닉네임 중복 검사
         if(isDuplicatedName(strData)) {
            System.out.println("이름 중복 입니다.");
            OutputStream os = socket.getOutputStream();
            data = "이름중복".getBytes();
            os.write(data);
            return;
         }
         else {
            System.out.println("이름 중복 아닙니다.");
            OutputStream os = socket.getOutputStream();
            data = "이름중복아님".getBytes();
            os.write(data);
         }

         name = strData;
         System.out.println("유저 이름 : " + name);

         // 이름까지 검증이 되면, 넣어준다.
         clientMangers.add(this);

         // 모든 클라이언트에게 접속을 알린다.
         sendMessage(null, "인원목록");

         // 클라이언트로부터 대화를 받고 보낸다.
         while(true) {

            System.out.println("클라이언트 대화 대기 중...");
            data = new byte[1024];
            size = is.read(data);
            if(size == -1) {
               System.out.println(name + " 클라이언트와 연결이 끊어짐.");
               removeClient(name);
               sendMessage(name, "로그아웃");
               sendMessage(null, "인원목록");
               break;
            }

            // 받은 대화를 모든 클라이언트들에게 전달한다.
            strData = new String(data, 0, size);
            sendMessage(strData, "대화내용");
         }

      } catch (IOException e) { e.printStackTrace(); }

   }

   /*
    * type : "대화내용", "인원목록"
    */
   int sendMessage(String message, String type) {
      String targetName = "";

      //메세지 종류별 플래그 추가
      switch(type) {
      case "인원목록" : message = type + "!" + getStrUserList(); break;
      case "로그아웃" : message = type + "!" + message + " 님이 나갔습니다."; break;
      case "대화내용" : 
         // 대화 종류 판별
         String[] strArrMessage = message.split(" ", 3);
         // 귓속말
         if(strArrMessage[0].equals("/r")) {
            targetName = strArrMessage[1];
            System.out.println("아이디 : " + targetName);
            message = type + "!" + name + " : " + strArrMessage[2]; 
            System.out.println("내용 : " + message);
            break;
         } 
         else {
            message = type + "!" + name + " : " + message; break;
         }
      }

      try {
         //** 코드 깔끔하게 수정할 것
         if(!targetName.isEmpty()) {
            for (ClientManger clientManger : clientMangers) {

               if(clientManger.name.equals(targetName) || clientManger.name.equals(this.name)) {
                  System.out.println("귓속말 대상");
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
      System.out.println("서버 오픈");

      new Thread() {
         @Override
         public void run() {

            try {

               serverSocket = new ServerSocket();
               if(serverSocket == null) {
                  System.out.println("서버 오픈 실패 : 서버 소켓을 생성할 수 없습니다.");
                  return;
               }
               System.out.println("서버 오픈 완료");

               serverSocket.bind(new InetSocketAddress(serverIP, port));

               // 클라이언트 소켓의 접근을 받는다.
               while(true) {
                  System.out.println("클라이언트 접속 대기");
                  Socket socket = serverSocket.accept();
                  System.out.println("클라이언트 접속");

                  ClientManger clientManger = new ClientManger(socket);
                  clientManger.start();
               }

            } catch(SocketException e) {
               System.out.println("소켓 연결 끊어짐.");
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

   // UI 생성
   void createMainPage() {

      VBox root = new VBox();
      root.setPrefSize(400, 400);

      // --- UI 컨트롤 ---

      Button btnOpenServer = new Button("서버 열기");
      btnOpenServer.setOnAction((arg0) -> {
         // 서버 열기
         serverManager.openServer();
      });

      Button btnCloseServer = new Button("서버 닫기");
      btnCloseServer.setOnAction((arg0) -> {
         // 서버 닫기
         // serverManager.stop();
         serverManager.closeServer();
      });

      root.getChildren().addAll(btnOpenServer, btnCloseServer);

      // --- UI 컨트롤 ---

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.setTitle("채팅 클라이언트");
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
      System.out.println("서버 프로그램 시작");
      launch();
   }

}