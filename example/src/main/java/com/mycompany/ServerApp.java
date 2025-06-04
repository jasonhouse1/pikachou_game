package com.mycompany;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class ServerApp extends Application {
    private Player player1;
    private Player player2;
    private Circle ball;
    private Rectangle net;
    private ObjectOutputStream out;

    private final Set<String> input = new HashSet<>();
    private final Set<String> clientInput = new HashSet<>();

    private final double sceneHeight = 400;
    private final double floorY = sceneHeight - 60;
    private double ballVX = 0;
    private double ballVY = 0;
    private boolean gameStarted = false;

    private int scoreP1 = 0;
    private int scoreP2 = 0;

    private Label scoreLabel1;
    private Label scoreLabel2;
    private Label winLabel;

    private boolean gameOver = false;

    private long spikeTimeP1 = 0;
    private long spikeTimeP2 = 0;
    private final long SPIKE_WINDOW = 1000;

    private long pauseUntil = 0;
    private Player nextServePlayer = null;

    @Override
    public void start(Stage primaryStage) {
        player1 = new Player(50, floorY, Color.RED);
        player2 = new Player(300, floorY, Color.BLUE);
        ball = new Circle(20, Color.ORANGE);
        resetBallAbove(player1);

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

        Pane root = new Pane(player1.body, player2.body, ball, net, scoreLabel1, scoreLabel2, winLabel);
        root.setPrefSize(400, sceneHeight);
        Scene scene = new Scene(root);

        scene.setOnKeyPressed(e -> input.add(e.getCode().toString()));
        scene.setOnKeyReleased(e -> input.remove(e.getCode().toString()));

        primaryStage.setTitle("Server (Player1)");
        primaryStage.setScene(scene);
        primaryStage.show();
        scene.getRoot().requestFocus();

        new Thread(this::startServer).start();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameOver) return;

                if (System.currentTimeMillis() < pauseUntil) {
                    sendGameState();
                    return;
                }

                if (nextServePlayer != null) {
                    resetBallAbove(nextServePlayer);
                    nextServePlayer = null;
                    return;
                }

                handlePlayer1();
                handlePlayer2();

                player1.applyGravity(0.75, floorY);
                player2.applyGravity(0.75, floorY);

                ballVY += 0.75;
                ball.setCenterX(ball.getCenterX() + ballVX * 0.4);
                ball.setCenterY(ball.getCenterY() + ballVY * 0.4);

                if (ball.getCenterY() - ball.getRadius() <= 0) {
                    ball.setCenterY(ball.getRadius());
                    ballVY = Math.abs(ballVY);
                }

                if (ball.getCenterY() + ball.getRadius() >= sceneHeight) {
                    ball.setCenterY(sceneHeight - ball.getRadius());
                    ballVY = -Math.abs(ballVY) * 1.2;
                    if (Math.abs(ballVY) < 1) ballVY = 0;

                    if (gameStarted) {
                        if (ball.getCenterX() < 200) {
                            scoreP2++;
                            updateScore();
                            checkWin();
                            pauseUntil = System.currentTimeMillis() + 5000;
                            nextServePlayer = player1; // 改為失分方發球
                        } else {
                            scoreP1++;
                            updateScore();
                            checkWin();
                            pauseUntil = System.currentTimeMillis() + 5000;
                            nextServePlayer = player2; // 改為失分方發球
                        }
                    }
                }

                if (ball.getCenterX() - ball.getRadius() <= 0 || ball.getCenterX() + ball.getRadius() >= 400) {
                    ballVX = -ballVX;
                }

                if (ball.getBoundsInParent().intersects(player1.body.getBoundsInParent())) {
                    if (!gameStarted) {
                        startGameFromCollision(player1);
                    } else {
                        if (System.currentTimeMillis() - spikeTimeP1 <= SPIKE_WINDOW) {
                            applySpike(player1, true);
                        } else {
                            applyBounce(player1);
                        }
                    }
                }

                if (ball.getBoundsInParent().intersects(player2.body.getBoundsInParent())) {
                    if (!gameStarted) {
                        startGameFromCollision(player2);
                    } else {
                        if (System.currentTimeMillis() - spikeTimeP2 <= SPIKE_WINDOW) {
                            applySpike(player2, false);
                        } else {
                            applyBounce(player2);
                        }
                    }
                }

                if (ball.getBoundsInParent().intersects(net.getBoundsInParent())) {
                    ballVX = -ballVX * 0.8;
                    ball.setCenterX(ball.getCenterX() + ballVX);
                }

                if (System.currentTimeMillis() - spikeTimeP1 > SPIKE_WINDOW && System.currentTimeMillis() - spikeTimeP2 > SPIKE_WINDOW) {
                    ball.setFill(Color.ORANGE);
                }

                sendGameState();
            }
        };
        timer.start();
    }

    private void applySpike(Player player, boolean isLeft) {
        ball.setFill(Color.RED);
        ballVX = isLeft ? 18 : -18;
        ballVY = 4;
    }

    private void updateScore() {
        scoreLabel1.setText("P1: " + scoreP1);
        scoreLabel2.setText("P2: " + scoreP2);
    }

    private void checkWin() {
        if (scoreP1 >= 5) {
            winLabel.setText("P1 Wins!");
            gameOver = true;
        } else if (scoreP2 >= 5) {
            winLabel.setText("P2 Wins!");
            gameOver = true;
        }
    }

    private void resetBallAbove(Player player) {
        gameStarted = false;
        ballVX = 0;
        ballVY = 0;
        ball.setFill(Color.ORANGE);
        ball.setCenterX(player.body.getX() + player.body.getWidth() / 2);
        ball.setCenterY(player.body.getY() - ball.getRadius() - 5);
    }

    private void startGameFromCollision(Player player) {
        gameStarted = true;
        double offset = ball.getCenterX() - player.body.getX() - player.body.getWidth() / 2;
        ballVX = offset * 0.07;
        ballVY = -1;
    }

    private void applyBounce(Player player) {
        double offset = ball.getCenterX() - player.body.getX() - player.body.getWidth() / 2;
        ballVX = offset * 0.8;
        ballVY = -23;
    }

    private void handlePlayer1() {
        double nextX = player1.body.getX();
        if (input.contains("A")) nextX -= 5;
        if (input.contains("D")) nextX += 5;
        if (nextX + player1.body.getWidth() <= 200 - net.getWidth()) {
            player1.body.setX(nextX);
        }
        if (input.contains("W") && player1.onGround) {
            player1.velocityY = -15;
            player1.onGround = false;
        }
        if (input.contains("SPACE")) {
            spikeTimeP1 = System.currentTimeMillis();
        }
    }

    private void handlePlayer2() {
        synchronized (clientInput) {
            double nextX = player2.body.getX();
            if (clientInput.contains("LEFT")) nextX -= 5;
            if (clientInput.contains("RIGHT")) nextX += 5;
            if (nextX >= 200 + net.getWidth() && nextX + player2.body.getWidth() <= 400) {
                player2.body.setX(nextX);
            }
            if (clientInput.contains("UP") && player2.onGround) {
                player2.velocityY = -15;
                player2.onGround = false;
            }
            if (clientInput.contains("ENTER")) {
                spikeTimeP2 = System.currentTimeMillis();
            }
        }
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            Socket clientSocket = serverSocket.accept();
            out = new ObjectOutputStream(clientSocket.getOutputStream());

            new Thread(() -> {
                try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
                    while (true) {
                        @SuppressWarnings("unchecked")
                        Set<String> inputSet = (Set<String>) in.readObject();
                        synchronized (clientInput) {
                            clientInput.clear();
                            clientInput.addAll(inputSet);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendGameState() {
        if (out != null) {
            try {
                double colorCode = ball.getFill().equals(Color.RED) ? 1 : 0;
                double[] data = {
                        player1.getX(), player1.getY(),
                        player2.getX(), player2.getY(),
                        ball.getCenterX(), ball.getCenterY(),
                        scoreP1, scoreP2,
                        gameOver ? 1 : 0,
                        colorCode
                };
                out.writeObject(data);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
