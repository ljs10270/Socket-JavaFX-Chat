package application;
	
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Main extends Application {
	
	Socket socket;
	TextArea textArea;
	
	//클라이언트 프로그램 동작 메소드
	public void startClient(String IP, int port) {
		//서버와는 다르게 쓰레드풀을 사용할 필요 x 그래서 Runnable이 아니라 그냥 Thread를 사용
		Thread thread = new Thread() {
			
			public void run() {
				try {
					socket = new Socket(IP, port);
					receive(); //서버로부터 메시지를 전달 받음
				} catch (Exception e) {
					if(!socket.isClosed()) {
						stopClient();
						System.out.println("[서버 접속 실패]");
						Platform.exit();
					}
				}
				
			}
		};
		thread.start();
	}
	
	//클라이언트 프로그램 종료 메소드
	public void stopClient() {
		try {
			if(socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	// 서버로부터 메세지를 전달받는 메소드
	public void receive() {
		while(true) {
			try { //서버와 동일 인풋스트림을 소켓을 이용해 열어서 똑같음
				InputStream in = socket.getInputStream(); //전달받는 변수
				byte[] buffer = new byte[512]; //한번에 512byte만큼만 전달받을 수 있게 할 것
				int length = in.read(buffer); //인풋스트림을 이용하여 전달 받은 내용을 바이트 버퍼에 담고 바이트 수를 반환한다.
				//length는 내용의 크기가 됨

				if(length == -1) throw new IOException();
				
				String message = new String(buffer, 0, length, "UTF-8");
				Platform.runLater(()->{
					textArea.appendText(message);
				});
				
			} catch(Exception e) {
				stopClient();
				break;
			}
		}
	}
	
	// 서버로 메세지를 전송하는 메소드
	public void send(String message) {
		Thread thread = new Thread() {
			
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer); //소켓의 아웃풋스트림에 출력(쓴다) 그럼 서버는 인풋스트림으로 소켓을 열어서 통신한다.
					out.flush();
				} catch(Exception e) {
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	//실제로 프로그램을 동작시키는 메소드
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		HBox hbox = new HBox();
		hbox.setSpacing(5);
		
		TextField userName = new TextField(); //사용자의 이름이 들어갈 공간
		userName.setPrefWidth(150);
		userName.setPromptText("닉네임을 입력하세요.");
		HBox.setHgrow(userName, Priority.ALWAYS);
		
		TextField IPText = new TextField("127.0.0.1"); //기본적으로 127.0.0.1로 설정
		TextField portText = new TextField("9876");
		portText.setPrefWidth(80);
		
		hbox.getChildren().addAll(userName, IPText, portText); //hbox에 위의 요소들 추가
		root.setTop(hbox);
		
		textArea = new TextArea();
		textArea.setEditable(false); //문장을 출력만 하고 수정 불가
		root.setCenter(textArea); //중간에 textArea를 담음
		
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		input.setDisable(true); //접속하기 버튼을 누르기 전까지는 메세지 전송 텍스트 입력 불가
		input.setOnAction(event-> {
			send(userName.getText() + ": " + input.getText() + "\n"); //사용자 이름과 입력한 내용을 서버로 보낸다
			input.setText(""); //서버로 메시지 보내고 초기화
			input.requestFocus(); //다시 메세지를 보낼 수 있게 포커스 설정
		});
		
		Button sendButton = new Button("보내기");
		sendButton.setDisable(true); //접속하기 이전에는 send버튼 비활성화
		
		sendButton.setOnAction(event-> {
			send(userName.getText() + ": " + input.getText() + "\n"); //사용자 이름과 입력한 내용을 서버로 보낸다
			input.setText(""); //서버로 메시지 보내고 초기화
			input.requestFocus(); //다시 메세지를 보낼 수 있게 포커스 설정
		});
		
		Button connectionButton = new Button("접속하기");
		connectionButton.setOnAction(event -> {
			if(connectionButton.getText().equals("접속하기")) {
				int port = 9876;
				
				try {
					port = Integer.parseInt(portText.getText());
				} catch(Exception e) {
					e.printStackTrace();
				}
				startClient(IPText.getText(), port); //접속
				Platform.runLater(() ->{
					textArea.appendText("[ 채팅방 접속 ]\n");
				});
				connectionButton.setText("종료하기");
				input.setDisable(false); //보낸 후 다시 입력가능하게 만들기
				sendButton.setDisable(false);
				input.requestFocus(); //바로 다른 메시지 입력 가능하게 포커싱 주기
			}
			else {
				stopClient();
				Platform.runLater(() -> {
					textArea.appendText("[ 채팅방 퇴장 ]\n");
				});
				connectionButton.setText("접속하기");
				input.setDisable(true);
				sendButton.setDisable(true);
			}
		});
		
		
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton);
		pane.setCenter(input);
		pane.setRight(sendButton);
		
		root.setBottom(pane);
		
		Scene scene = new Scene(root,400,400);
		primaryStage.setTitle("[채팅 클라이언트]");
		primaryStage.setOnCloseRequest(event -> stopClient()); //프로그램을 완전히 종료했다면 stopClient를 호출한 뒤에 종료
		primaryStage.setScene(scene);
		primaryStage.show();
		
		connectionButton.requestFocus(); //프로그램이 실행되면 접속하기 버튼이 기본적으로 포커싱 됨
		
		
		
		/*
		try {
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root,400,400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		*/
	}
	
	public static void main(String[] args) {
		launch(args);
		
	}
}
