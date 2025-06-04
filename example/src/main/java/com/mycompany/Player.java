package com.mycompany;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player {
    public Rectangle body;
    public double velocityX = 0;
    public double velocityY = 0;
    public boolean onGround = true;

    public void setPosition(double x, double y) {
        body.setX(x);
        body.setY(y);
    }

    public Player(double x, double y, Color color) {
        body = new Rectangle(40, 60, color);
        body.setX(x);
        body.setY(y);
    }

    public void moveX(double dx) {
        velocityX = dx;
        body.setX(body.getX() + dx);
    }

    public void moveY(double dy) {
        body.setY(body.getY() + dy);
    }

    public void applyGravity(double gravity, double floorY) {
        velocityY += gravity;
        moveY(velocityY);

        if (body.getY() >= floorY) {
            body.setY(floorY);
            velocityY = 0;
            onGround = true;
        } else {
            onGround = false;
        }
    }

    public double getX() {
        return body.getX();
    }

    public double getY() {
        return body.getY();
    }
}
