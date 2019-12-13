package application;

import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Main extends Application implements Runnable{
	int serverHost = 0;
	Thread thread;
	Circle[][] circles = new Circle[6][7];
	boolean win = false;
	BorderPane root = new BorderPane();
	Scene scene = new Scene(root, 690, 590);
	BorderPane s = new BorderPane();
	Scene start = new Scene(s, 690, 590);
	boolean cantConnect = false;
	boolean yourTurn = false;
	DataInputStream dis;
	DataOutputStream dos;
	Socket socket;
	ServerSocket serverSocket;
	String ip = "localhost";
	int port = 53974;
	boolean accepted = false;
	boolean connected = false;

	@Override
	public void start(Stage primaryStage) {
		try {
			System.out.println("Enter a port between 1-60000");
			Scanner in = new Scanner(System.in);
			if(!in.hasNextInt()) {
				System.out.println("Please enter a port number");
				System.exit(0);
			}
			port = in.nextInt();
			scene.setFill(Color.BLUE);
			createCircles();
			for (int j = 0; j < 7; j++) {
				for (int i = 0; i < 6; i++) {
					root.getChildren().add(circles[i][j]);
				}
			}
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
			primaryStage.show();
			if(!connect()) {
				System.out.println("Started a server");
				server();
			}
			while(!accepted) {
				listen();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		thread = new Thread(this);
		thread.start();

	}

	public static void main(String[] args) {
		launch(args);
	}

	public void createCircles() {
		for (int j = 0; j < 7; j++) {
			for (int i = 0; i < 6; i++) {
				Circle c = new Circle(j * 100 + 50, i * 100 + 50, 45, Color.WHITE);
				circles[i][j] = c;
				c.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					System.out.println(accepted + " " + yourTurn);
			 			if (yourTurn && accepted) {
			 				yourTurn = false;
							fillEmpty(c.getCenterX(), c.getCenterY());	
					}
				});
			}
		}
	}

	public void fillEmpty(double row, double column) {
		int y = (int) column;
		int x = (int) row;
		y = y / 100;
		x = x / 100;
		int z1 = 0;
		int x1 = 0;
		int place = 0;
		for (int z = 5; z >= 0; z--) {
			if (circles[z][x].getFill().equals(Color.WHITE)) {
				if (serverHost == 0) {
					circles[z][x].setFill(Color.YELLOW);
					if (checkWin(z, x, Color.YELLOW)) {
						win();
					}
				} else {
					circles[z][x].setFill(Color.RED);
					if (checkWin(z, x, Color.RED)) {
						win();
					}
				}
				z1 = z;
				x1 = x;
				place = z1*10 + x1;
  				break;
			}
		}
		try {
			dos.writeInt(place);
			dos.flush();
			dis.notify();
		} catch (Exception e) {

		}
	}

	public boolean checkWin(int x, int y, Paint color) {
		return checkDiagonal(x, y, color) || checkHorizontal(x, y, color) || checkVertical(x, y, color);
	}

	public boolean checkDiagonal(int x, int y, Paint color) {
		int[][] dir = new int[][] { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
		for (int[] d : dir) {
			int di = d[0];
			int dj = d[1];
			if (x + 3 * di < 6 && x + 3 * di >= 0 && y + 3 * dj >= 0 && y + 3 * dj < 7) {
				if (circles[x + di][y + dj].getFill().equals(color)) {
					if (circles[x + 2 * di][y + 2 * dj].getFill().equals(color)) {
						if (circles[x + 3 * di][y + 3 * dj].getFill().equals(color)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean checkHorizontal(int x, int y, Paint color) {
		int[][] dir = new int[][] { { 0, 1 }, { 0, -1 } };
		for (int[] d : dir) {
			int dj = d[1];
			if (y + 3 * dj < 7 && y + 3 * dj >= 0) {
				if (circles[x][y + dj].getFill().equals(color)) {
					if (circles[x][y + 2 * dj].getFill().equals(color)) {
						if (circles[x][y + 3 * dj].getFill().equals(color)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean checkVertical(int x, int y, Paint color) {
		int[][] dir = new int[][] { { 1, 0 }, { -1, 0 } };
		for (int[] d : dir) {
			int di = d[0];
			if (x + 3 * di < 6 && x + 3 * di >= 0) {
				if (circles[x + di][y].getFill().equals(color)) {
					if (circles[x + 2 * di][y].getFill().equals(color)) {
						if (circles[x + 3 * di][y].getFill().equals(color)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public void freezeFrame() {
		createCircles();
		for (int j = 0; j < 7; j++) {
			for (int i = 0; i < 6; i++) {
				Circle c = new Circle(j * 100 + 50, i * 100 + 50, 45, circles[i][j].getFill());
				circles[i][j] = c;
			}
		}
	}

	public void win() {
		freezeFrame();
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText(null);
		alert.setTitle("Connect Four");
		if (yourTurn) {
			alert.setContentText("Player two wins");
		} else
			alert.setContentText("Player one wins");
		alert.showAndWait();
	}

	
	private void server() {
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
		serverHost = 1;
	}
	
	@Override
	public void run() {
		while(true) {
		while(!accepted) {
			if(!yourTurn && !accepted) {
				listen();
			}
		}
		check();
		System.out.println();
		}
	}

	private void check() {
		int e = 0;
		if (e >= 10) {
			cantConnect = true;
		}
		if (!yourTurn && accepted) {
			try {
				System.out.println("waiting");
				String placeholder = Integer.toString(dis.readInt());
				System.out.println(placeholder);
				if(serverHost == 0) {
					circles[Character.getNumericValue(placeholder.charAt(0))][Character.getNumericValue(placeholder.charAt(1))].setFill(Color.RED);
					if(checkWin(Character.getNumericValue(placeholder.charAt(0)), Character.getNumericValue(placeholder.charAt(1)), Color.RED)) win();;
				} else {
					circles[Character.getNumericValue(placeholder.charAt(0))][Character.getNumericValue(placeholder.charAt(1))].setFill(Color.YELLOW);
					if(checkWin(Character.getNumericValue(placeholder.charAt(0)), Character.getNumericValue(placeholder.charAt(1)), Color.YELLOW)) win();
				}
					
					yourTurn = true;
			} catch(IOException a) {
				a.printStackTrace();
				e++;
			}
		}
	}
	
	private void listen() {
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			if(dis.readInt() == 1) {
				accepted = true;
			}
			accepted = true;
			System.out.println("Connected to another player");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean connect() {
		try {
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
			dos.writeInt(1);	
		} catch (Exception e) {
			System.out.println("Unable to connect");
			return false;
		}
		System.out.println("Connected");
		return true;
	}

}