package Chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/*
 * 채팅 프로그램 [클라이언트]
 * 1. 서버로 채팅 내용을 전송
 * 2. 서버로부터 다른 클라이언트의 채팅 내용을 전달 받아 화면에 띄운다.
 * 3. 로그인 시 닉네임 입력
 *  1) 닉네임 제한 : 최대 5자
 *  2) 닉네임 중복 불가능
 * 4. 서버로부터 접속 중인 닉네임 목록을 받아서 화면에 표시한다.
 * 5. 귓속말을 보낼 수 있다. (명령어 : /r 아이디 내용)
 */

/*
 * 메인 페이지
 *  - 로그인 페이지랑 상속 받도록 바꿀 것
 */
class MainPage {
   Stage mainStage;
   TextArea taMessage;
   VBox vbUserList;
   SocketManager socketManager;

   MainPage(String name){
      
      // 닉네임 검사
      if(name.equals("") || (name.indexOf(" ") != -1) || (name.length() > socketManager.maxNameLen)) {
         showAlert(AlertType.WARNING, 
               "경고", "맞을래요?", "공백을 제외한 최대 5자리까지 입력 가능합니다.");
         return;
      }

      socketManager = new SocketManager(SocketManager.serverIP, SocketManager.port);
      int isConnected = socketManager.connect();
      if(isConnected == 1) {

         // 로그인 정보
         socketManager.name = name;
         sendData(socketManager.name);
         
         // 닉네임 중복 검사
         try {
            InputStream is = socketManager.socket.getInputStream();
            byte[] data = new byte[1024];
            int size = is.read(data);
            if(size == -1) {
               return;
            }
            
            String strData = new String(data, 0, size);
            if(strData.equals("이름중복")) {
               showAlert(AlertType.WARNING, 
                     "경고", "맞을래요?", "이름 중복");
               return;
            }
            else {
               System.out.println("이름 중복 아님");
            }
            
         } catch (IOException e) { e.printStackTrace(); }

         // 메인 화면으로 전환
         mainStage = new Stage();
         createMainPage();
         LoginPage.loginStage.close();

         // 서버로부터 데이터 받는 스레드
         recieveData();

      }
      else {
         System.out.println("서버 연결 실패");
      }

   }

   void showAlert(AlertType type, String title, String header, String content) {
      Alert alert = new Alert(type);
      alert.setTitle("경고");
      alert.setHeaderText("맞을래요?");
      alert.setContentText("이름 중복");
      alert.showAndWait();
   }

   void createMainPage() {

      VBox root = new VBox();
      root.setPrefSize(400, 400);

      // --- UI 컨트롤 ---

      // -- Header
      HBox header = new HBox();

      Label lblTitle = new Label("시크릿 채팅방");
      lblTitle.setPrefSize(200, 50);

      Label lblUserName = new Label("[ " + socketManager.name + " ]");
      lblUserName.setPrefSize(200, 50);
      lblUserName.setAlignment(Pos.BOTTOM_RIGHT);

      header.getChildren().addAll(lblTitle, lblUserName);
      // -- Header


      // -- Body
      HBox body = new HBox();

      taMessage = new TextArea();
      taMessage.setMinWidth(300);

      vbUserList = new VBox();
      vbUserList.setMinWidth(100);

      body.getChildren().addAll(taMessage, vbUserList);
      // -- Body


      // -- Bottom
      HBox bottom = new HBox();

      Button btnLogout = new Button("나가기");
      btnLogout.setOnAction((arg0) -> {
         // 로그인 화면으로 전환
         logout();
         changePage();
      });

      TextField tfContent = new TextField();
      tfContent.setOnAction((arg0)->{
         // 대화 내용 전달하기
         sendData(tfContent.getText());
         tfContent.setText("");
      });
      Platform.runLater(()-> { tfContent.requestFocus(); }); // 포커스

      Button btnSend = new Button("보내기");
      btnSend.setOnAction((arg0) -> {
         // 대화 내용 전달하기
         sendData(tfContent.getText());
         tfContent.setText("");
      });

      bottom.getChildren().addAll(btnLogout, tfContent, btnSend);
      // -- Bottom

      root.getChildren().addAll(header, body, bottom);

      // --- UI 컨트롤 ---

      Scene scene = new Scene(root);
      mainStage.setScene(scene);
      mainStage.setTitle("대화방");
      mainStage.setResizable(false);
      mainStage.setOnCloseRequest(event -> { 
         //windowContainer.showClosingPopup("Exit"); 
         terminate(); 
      });
      mainStage.show();
   }

   void recieveData() {
      // 서버로부터 데이터 받기
      new Thread() {
         public void run() {
            try {
               InputStream is = socketManager.socket.getInputStream();
               byte[] data;

               while(true) {
                  System.out.println("데이터 받기 대기 중...");

                  // 서버로부터 데이터 받기
                  data = new byte[1024];
                  int size = is.read(data);
                  if(size == -1) {
                     System.out.println("서버 연결 끊김");
                     break;
                  }
                  System.out.println("데이터 받기 완료");

                  // 전처리
                  String strData = new String(data, 0, size);
                  System.out.println("받은 데이터 : " +  strData);
                  String type = strData.split("!", 2)[0];
                  String message = strData.split("!", 2)[1];

                  switch(type) {
                  case "인원목록" : updateUserList(message); break; 
                  case "로그아웃" :
                  case "대화내용" : updateMessage(message); break; 
                  }

               }

            } catch (SocketException e) {
               System.out.println("소켓 연결 끊어짐.");
            } catch (IOException e) { e.printStackTrace(); }

         };
      }.start();
   }

   String byte2String(byte[] data) {
      return new String(data, 0, data.length);
   }

   // *** 수정하기 : openPage로?
   void changePage() {
      // 메인창 닫기
      mainStage.close();

      // 로그인창 열기
      new LoginPage(new Stage());
   }
   
   void logout() {
      socketManager.disconnect();
   }

   void updateMessage(String message) {
      System.out.println("받은 메세지 내용 : " + message);

      // 채팅창에 올리기
      Platform.runLater(()-> { 
         taMessage.appendText(message + "\n");
      });
   }

   void updateUserList(String strUserList) {
      System.out.println("유저 리스트 새로고침");
      String[] strUserArr = strUserList.split(" ");

      System.out.println(strUserArr.length);

      // UI 관련 사항은 javaFx 스레드가 처리하도록 한다.
      Platform.runLater(()-> { 

         // 유저 리스트에 내용 올리기
         vbUserList.getChildren().clear();

         for (String userName : strUserArr) {
            vbUserList.getChildren().add(new Label(userName));   
         }

      });

   }

   void sendData(String message) {
      System.out.println("보낸 메세지 내용 : " + message);

      try {
         OutputStream os = socketManager.socket.getOutputStream();
         byte[] data = message.getBytes();
         os.write(data);

      } catch (IOException e) { e.printStackTrace(); }

   }

   void terminate() {
      logout();
      System.exit(0);
   }
}

/*
 * 클라이언트 소켓
 */
class SocketManager {
   static String serverIP = "localhost";
   static int port = 5001;
   int timeOut = 10000;
   Socket socket;
   String name;
   static int maxNameLen = 5;

   public SocketManager(String serverIP, int port) {
      this.serverIP = serverIP;
      this.port = port;
   }

   int connect() {
      System.out.println("클라이언트 접속 시도");

      try {

         socket = new Socket();
         socket.connect(new InetSocketAddress(serverIP, port), timeOut);

         // catch문 좀더 개선하기
      } catch (UnknownHostException e) {
         //         e.printStackTrace();
         return -1;
      } catch (SocketTimeoutException e) {
         System.out.println("서버가 응답이 없음.");
         //         e.printStackTrace();
         return -1;
      } catch (IOException e) {
         //         e.printStackTrace();
         return -1;
      }

      return 1;

   }
   
   void disconnect() {
      try {
         socket.close();
      } catch (IOException e) { e.printStackTrace(); }
   }
}

/*
 * 로그인 페이지
 *  - 메인 페이지랑 상속 받도록 바꿀 것
 */
class LoginPage {
   static Stage loginStage;
   TextField tfName;

   LoginPage(Stage stage) {
      this.loginStage = stage;
      createLoginPage();
   }

   void createLoginPage() {

      VBox root = new VBox();
      root.setPrefSize(400, 400);

      // --- UI 컨트롤 ---

      Label lblTitle = new Label("시크릿 채팅방");
      Label lblWarning = new Label("최대 5글자까지 입력 가능합니다.");

      tfName = new TextField();
      tfName.setOnAction((arg0)-> {
         // 접속
         changePage();
      });

      Button btnConnect = new Button("접속");
      btnConnect.setOnAction((arg0) -> {
         // 접속
         changePage();
      });

      root.getChildren().addAll(lblTitle, lblWarning, tfName, btnConnect);

      // --- UI 컨트롤 ---

      Scene scene = new Scene(root);
      loginStage.setScene(scene);
      loginStage.setTitle("로그인");
      loginStage.setResizable(false);
      loginStage.setOnCloseRequest(event -> { 
         //windowContainer.showClosingPopup("Exit"); 
         terminate(); 
      });
      loginStage.show();
   }

   void changePage() {

      // 메인화면으로 전환
      new MainPage(tfName.getText());

   }
   
   void terminate() {
      System.exit(0);
   }
}

/*
 * 메인 클래스
 */
public class Client extends Application {
   @Override
   public void start(Stage stage) throws Exception {
      new LoginPage(stage);
   }

   public static void main(String[] args) {
      System.out.println("클라이언트 프로그램 시작");
      launch();
   }

}