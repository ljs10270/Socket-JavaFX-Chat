package application;

import java.io.IOException;
import java.net.Socket;

import java.io.InputStream;
import java.io.OutputStream;

public class Client { //서버가 하나의 클라이언트와 통신하기 위한 기능을 정의
	Socket socket;
	
	public Client(Socket socket) {
		this.socket = socket;
		receive();
	}
	
	//클라이언트로부터 메세지를 전달 받는 메소드
	public void receive() {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						InputStream in = socket.getInputStream(); //전달받는 변수
						byte[] buffer = new byte[512]; //한번에 512byte만큼만 전달받을 수 있게 할 것
						int length = in.read(buffer); //인풋스트림을 이용하여 전달 받은 내용을 바이트 버퍼에 담고 바이트 수를 반환한다.
						//length는 내용의 크기가 됨
	
						if(length == -1) throw new IOException();
						
						System.out.println("[메세지 수신 성공]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						
						String message = new String(buffer, 0, length, "UTF-8"); //전달받은 내용을 인코딩 하여 문자열 변수에 대입
						
						for(Client client : Main.clients) { //전달받은 메세지를 다른 클라이언트들에게도 통신 가능하게 함
							client.send(message);
						}
					}
				} catch(Exception e) {
					try {
						System.out.println("[메세지 수신 오류]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						Main.clients.remove(Client.this);
						socket.close();
					} catch(Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadPooL.submit(thread); //Main클레스의 스레드풀 변수에 
		//생성되는 스레드를 안정적으로 관리하기 위해  스레드를 등록
	}
	
	// 클라이언트에게 메세지를 전송하는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8"); //메세지 내용을 인코딩하여 버퍼 배열에 담는다.
					out.write(buffer); //버퍼의 내용을 출력
					out.flush(); //필수
				} catch (IOException e) {
					try {
						System.out.println("[메세지 송신 오류]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						Main.clients.remove(Client.this); //메인클레스가 추가된 오류가 뜬 클라이언트를 제거
						socket.close();
					} catch(Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadPooL.submit(thread);
	}
}
