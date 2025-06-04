package com.mycompany;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ClientApp extends Application {
    public static String serverIP = "163.13.16.121";

    private Player player1;
    private Player player2;
    private Circle ball;
    private Rectangle net;

    private final Set<String> input = new HashSet<>();
    private ObjectOutputStream out;

    private Label scoreLabel1;
    private Label scoreLabel2;
    private Label winLabel;
    private Label pauseLabel;

    private volatile double[] lastReceivedData = null;

    @Override
    public void start(Stage primaryStage) {
        double sceneHeight = 400;

        player1 = new Player(50, sceneHeight - 60, Color.RED);
        player2 = new Player(300, sceneHeight - 60, Color.BLUE);
        ball = new Circle(20, Color.ORANGE);

        net = new Rectangle(5, 60, Color.DARKGRAY);
        net.setX(200 - 2.5);
        net.setY(sceneHeight - net.getHeight());

        scoreLabel1 = new Label("P1: 0");
        scoreLabel1.setTextFill(Color.RED);
        scoreLabel1.setFont(new Font(20));
        scoreLabel1.setLayoutX(10);
        scoreLabel1.setLayoutY(10);

        scoreLabel2 = new Label("P2: 0");
        scoreLabel2.setTextFill(Color.BLUE);
        scoreLabel2.setFont(new Font(20));
        scoreLabel2.setLayoutX(330);
        scoreLabel2.setLayoutY(10);

        winLabel = new Label("");
        winLabel.setFont(new Font(40));
        winLabel.setTextFill(Color.GREEN);
        winLabel.setLayoutX(120);
        winLabel.setLayoutY(180);

        pauseLabel = new Label("");
        pauseLabel.setFont(new Font(20));
        pauseLabel.setTextFill(Color.DARKGRAY);
        pauseLabel.setLayoutX(110);
        pauseLabel.setLayoutY(50);

        Pane root = new Pane(player1.body, player2.body, ball, net, scoreLabel1, scoreLabel2, winLabel, pauseLabel);
        root.setPrefSize(400, sceneHeight);
        Scene scene = new Scene(root);

        scene.setOnKeyPressed(e -> input.add(e.getCode().toString()));
        scene.setOnKeyReleased(e -> input.remove(e.getCode().toString()));

        primaryStage.setTitle("Client (Player2)");
        primaryStage.setScene(scene);
        primaryStage.show();
        scene.getRoot().requestFocus();

        new Thread(this::startClient).start();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    if (out != null) {
                        out.writeObject(new HashSet<>(input));
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (lastReceivedData != null) {
                    double p1x = lastReceivedData[0];
                    double p1y = lastReceivedData[1];
                    double p2x = lastReceivedData[2];
                    double p2y = lastReceivedData[3];
                    double ballX = lastReceivedData[4];
                    double ballY = lastReceivedData[5];
                    int scoreP1 = (int) lastReceivedData[6];
                    int scoreP2 = (int) lastReceivedData[7];
                    boolean gameOver = lastReceivedData[8] == 1;
                    boolean isSpiked = lastReceivedData[9] == 1;
                    boolean isPaused = lastReceivedData.length > 10 && lastReceivedData[10] == 1;

                    player1.setPosition(p1x, p1y);
                    player2.setPosition(p2x, p2y);
                    ball.setCenterX(ballX);
                    ball.setCenterY(ballY);
                    ball.setFill(isSpiked ? Color.RED : Color.ORANGE);
                    scoreLabel1.setText("P1: " + scoreP1);
                    scoreLabel2.setText("P2: " + scoreP2);
                    winLabel.setText(gameOver ? (scoreP1 >= 5 ? "P1 Wins!" : "P2 Wins!") : "");
                    pauseLabel.setText(isPaused ? "下一球即將開始..." : "");
                }
            }
        };
        timer.start();
    }

    private void startClient() {
        try (Socket socket = new Socket(serverIP, 12345)) {
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                double[] data = (double[]) in.readObject();
                lastReceivedData = data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
