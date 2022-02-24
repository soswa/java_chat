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
 * ä�� ���α׷� [Ŭ���̾�Ʈ]
 * 1. ������ ä�� ������ ����
 * 2. �����κ��� �ٸ� Ŭ���̾�Ʈ�� ä�� ������ ���� �޾� ȭ�鿡 ����.
 * 3. �α��� �� �г��� �Է�
 *  1) �г��� ���� : �ִ� 5��
 *  2) �г��� �ߺ� �Ұ���
 * 4. �����κ��� ���� ���� �г��� ����� �޾Ƽ� ȭ�鿡 ǥ���Ѵ�.
 * 5. �ӼӸ��� ���� �� �ִ�. (��ɾ� : /r ���̵� ����)
 */

/*
 * ���� ������
 *  - �α��� �������� ��� �޵��� �ٲ� ��
 */
class MainPage {
   Stage mainStage;
   TextArea taMessage;
   VBox vbUserList;
   SocketManager socketManager;

   MainPage(String name){
      
      // �г��� �˻�
      if(name.equals("") || (name.indexOf(" ") != -1) || (name.length() > socketManager.maxNameLen)) {
         showAlert(AlertType.WARNING, 
               "���", "��������?", "������ ������ �ִ� 5�ڸ����� �Է� �����մϴ�.");
         return;
      }

      socketManager = new SocketManager(SocketManager.serverIP, SocketManager.port);
      int isConnected = socketManager.connect();
      if(isConnected == 1) {

         // �α��� ����
         socketManager.name = name;
         sendData(socketManager.name);
         
         // �г��� �ߺ� �˻�
         try {
            InputStream is = socketManager.socket.getInputStream();
            byte[] data = new byte[1024];
            int size = is.read(data);
            if(size == -1) {
               return;
            }
            
            String strData = new String(data, 0, size);
            if(strData.equals("�̸��ߺ�")) {
               showAlert(AlertType.WARNING, 
                     "���", "��������?", "�̸� �ߺ�");
               return;
            }
            else {
               System.out.println("�̸� �ߺ� �ƴ�");
            }
            
         } catch (IOException e) { e.printStackTrace(); }

         // ���� ȭ������ ��ȯ
         mainStage = new Stage();
         createMainPage();
         LoginPage.loginStage.close();

         // �����κ��� ������ �޴� ������
         recieveData();

      }
      else {
         System.out.println("���� ���� ����");
      }

   }

   void showAlert(AlertType type, String title, String header, String content) {
      Alert alert = new Alert(type);
      alert.setTitle("���");
      alert.setHeaderText("��������?");
      alert.setContentText("�̸� �ߺ�");
      alert.showAndWait();
   }

   void createMainPage() {

      VBox root = new VBox();
      root.setPrefSize(400, 400);

      // --- UI ��Ʈ�� ---

      // -- Header
      HBox header = new HBox();

      Label lblTitle = new Label("��ũ�� ä�ù�");
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

      Button btnLogout = new Button("������");
      btnLogout.setOnAction((arg0) -> {
         // �α��� ȭ������ ��ȯ
         logout();
         changePage();
      });

      TextField tfContent = new TextField();
      tfContent.setOnAction((arg0)->{
         // ��ȭ ���� �����ϱ�
         sendData(tfContent.getText());
         tfContent.setText("");
      });
      Platform.runLater(()-> { tfContent.requestFocus(); }); // ��Ŀ��

      Button btnSend = new Button("������");
      btnSend.setOnAction((arg0) -> {
         // ��ȭ ���� �����ϱ�
         sendData(tfContent.getText());
         tfContent.setText("");
      });

      bottom.getChildren().addAll(btnLogout, tfContent, btnSend);
      // -- Bottom

      root.getChildren().addAll(header, body, bottom);

      // --- UI ��Ʈ�� ---

      Scene scene = new Scene(root);
      mainStage.setScene(scene);
      mainStage.setTitle("��ȭ��");
      mainStage.setResizable(false);
      mainStage.setOnCloseRequest(event -> { 
         //windowContainer.showClosingPopup("Exit"); 
         terminate(); 
      });
      mainStage.show();
   }

   void recieveData() {
      // �����κ��� ������ �ޱ�
      new Thread() {
         public void run() {
            try {
               InputStream is = socketManager.socket.getInputStream();
               byte[] data;

               while(true) {
                  System.out.println("������ �ޱ� ��� ��...");

                  // �����κ��� ������ �ޱ�
                  data = new byte[1024];
                  int size = is.read(data);
                  if(size == -1) {
                     System.out.println("���� ���� ����");
                     break;
                  }
                  System.out.println("������ �ޱ� �Ϸ�");

                  // ��ó��
                  String strData = new String(data, 0, size);
                  System.out.println("���� ������ : " +  strData);
                  String type = strData.split("!", 2)[0];
                  String message = strData.split("!", 2)[1];

                  switch(type) {
                  case "�ο����" : updateUserList(message); break; 
                  case "�α׾ƿ�" :
                  case "��ȭ����" : updateMessage(message); break; 
                  }

               }

            } catch (SocketException e) {
               System.out.println("���� ���� ������.");
            } catch (IOException e) { e.printStackTrace(); }

         };
      }.start();
   }

   String byte2String(byte[] data) {
      return new String(data, 0, data.length);
   }

   // *** �����ϱ� : openPage��?
   void changePage() {
      // ����â �ݱ�
      mainStage.close();

      // �α���â ����
      new LoginPage(new Stage());
   }
   
   void logout() {
      socketManager.disconnect();
   }

   void updateMessage(String message) {
      System.out.println("���� �޼��� ���� : " + message);

      // ä��â�� �ø���
      Platform.runLater(()-> { 
         taMessage.appendText(message + "\n");
      });
   }

   void updateUserList(String strUserList) {
      System.out.println("���� ����Ʈ ���ΰ�ħ");
      String[] strUserArr = strUserList.split(" ");

      System.out.println(strUserArr.length);

      // UI ���� ������ javaFx �����尡 ó���ϵ��� �Ѵ�.
      Platform.runLater(()-> { 

         // ���� ����Ʈ�� ���� �ø���
         vbUserList.getChildren().clear();

         for (String userName : strUserArr) {
            vbUserList.getChildren().add(new Label(userName));   
         }

      });

   }

   void sendData(String message) {
      System.out.println("���� �޼��� ���� : " + message);

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
 * Ŭ���̾�Ʈ ����
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
      System.out.println("Ŭ���̾�Ʈ ���� �õ�");

      try {

         socket = new Socket();
         socket.connect(new InetSocketAddress(serverIP, port), timeOut);

         // catch�� ���� �����ϱ�
      } catch (UnknownHostException e) {
         //         e.printStackTrace();
         return -1;
      } catch (SocketTimeoutException e) {
         System.out.println("������ ������ ����.");
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
 * �α��� ������
 *  - ���� �������� ��� �޵��� �ٲ� ��
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

      // --- UI ��Ʈ�� ---

      Label lblTitle = new Label("��ũ�� ä�ù�");
      Label lblWarning = new Label("�ִ� 5���ڱ��� �Է� �����մϴ�.");

      tfName = new TextField();
      tfName.setOnAction((arg0)-> {
         // ����
         changePage();
      });

      Button btnConnect = new Button("����");
      btnConnect.setOnAction((arg0) -> {
         // ����
         changePage();
      });

      root.getChildren().addAll(lblTitle, lblWarning, tfName, btnConnect);

      // --- UI ��Ʈ�� ---

      Scene scene = new Scene(root);
      loginStage.setScene(scene);
      loginStage.setTitle("�α���");
      loginStage.setResizable(false);
      loginStage.setOnCloseRequest(event -> { 
         //windowContainer.showClosingPopup("Exit"); 
         terminate(); 
      });
      loginStage.show();
   }

   void changePage() {

      // ����ȭ������ ��ȯ
      new MainPage(tfName.getText());

   }
   
   void terminate() {
      System.exit(0);
   }
}

/*
 * ���� Ŭ����
 */
public class Client extends Application {
   @Override
   public void start(Stage stage) throws Exception {
      new LoginPage(stage);
   }

   public static void main(String[] args) {
      System.out.println("Ŭ���̾�Ʈ ���α׷� ����");
      launch();
   }

}