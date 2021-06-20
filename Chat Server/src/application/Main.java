package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;


public class Main extends Application {
	
	public static ExecutorService threadPooL; //스레드풀을 이용하여 여러 클라이언트가 접속하였을 떄 
	//여러 스레드들을 효율적으로 접근하도록 함 갑자기 클라이언트의 수가 폭주하여도 이를 방지함
	
	public static Vector<Client> clients = new Vector<Client>(); //클라이언트들을 담는 벡터
	
	ServerSocket serverSocket;
	
	//서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP, int port) {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port));
		} catch (Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) { //서버 소켓이 닫혀있는 상태가 아니라면
				stopServer();
			}
			return;
		}
		
		//클라이언트가 접속할 때까지 기다리는 쓰레드
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept(); //클라이언트가 접속을 했다면
						clients.add(new Client(socket));
						
						System.out.println("[클라이언트 접속]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					} catch (Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
				
			}
		};
		threadPooL = Executors.newCachedThreadPool(); //스레드 풀 초기화
		threadPooL.submit(thread);
	}
	
	// 서버의 작동을 중지시키는 메소드
	public void stopServer() {
		try {
			Iterator<Client> iterator = clients.iterator(); //반복자 정의
			
			while(iterator.hasNext()) { //이터레이터로 클라이언트 하나씩 접근
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			// 서버 소켓 객체 닫기
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			// 쓰레드 풀 종료하기
			if(threadPooL != null && !threadPooL.isShutdown()) {
				threadPooL.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// UI를 생성하고, 실제로 프로그램을 작동시키는 메소드
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false); //문장을 출력만 하고 수정 불가
		textArea.setFont(new Font("나눔고딕", 15));
		root.setCenter(textArea); //중간에 textArea를 담음
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1,0,0,0));
		root.setBottom(toggleButton); //아래위치에 버튼을 담음
		
		String IP = "127.0.0.1"; //자신의 로컬 주소, 테스트
		int port = 9876;
		
		toggleButton.setOnAction(event -> { //사용자가 버튼을 눌렀을 떄 발생하는 이벤트 처리
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP, port); //서버 시작
				Platform.runLater(() -> { //필수! - ui요소를 출력
					String message = String.format("[서버 시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
				});
			} 
			else { //종료하기 버튼을 누른 경우
				stopServer();
				Platform.runLater(() -> { //필수! - ui요소를 출력
					String message = String.format("[서버 종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");
				});
			}
		});
		
		Scene scene = new Scene(root,400,400);
		primaryStage.setTitle("[채팅 서버]");
		primaryStage.setOnCloseRequest(event -> stopServer()); //프로그램을 완전히 종료했다면 stopServer를 호출한 뒤에 종료
		primaryStage.setScene(scene);
		primaryStage.show();
		
		/*
		try {
			BorderPane root = new BorderPane(); //보더펜 객체(레이아웃)인 디자인 요소를 이용해서
			Scene scene = new Scene(root,1280,720); //씬 창을 크기에 맞게 올린다.
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			//씬 창에 css 파일을 적용
			primaryStage.setScene(scene); //씬 등록
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		*/ //기본 제공 UI 코드들
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
