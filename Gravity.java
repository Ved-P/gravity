import java.awt.Graphics;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Gravity {

  public static void main(String[] args) {
    JFrame frame = new JFrame("Gravity");
    GravityPanel gp = new GravityPanel();
		frame.setSize(gp.length, gp.height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(10, 10);
		frame.setResizable(false);
		frame.add(gp);
		frame.setVisible(true);
  }

}

class GravityPanel extends JPanel {

  public int length, height;
  public double dt;
  public Planet[] planets;
  public Rocket[] rockets;
  public Timer timer;
  public double time;
  public int generation;
  public double weightRange;
  public int numInputs;
  public double[][] bestWeights;

  public GravityPanel() {
    length = 1200;
    height = 700;
    dt = 0.01;
    planets = new Planet[]{
      new Planet(600, 500, 100, 10000000),
      new Planet(200, 200, 50, 5000000),
      new Planet(1000, 100, 10, 1000000)
    };
    rockets = new Rocket[20];
    TimerHandler handler = new TimerHandler();
    timer = new Timer((int)(dt * 1000), handler);
    timer.start();
    time = 0;
    generation = 0;
    weightRange = 100.0;
    // numInputs = 8;
    // bestWeights = new double[2][numInputs];
    // for (int i = 0; i < 2; i++) {
    //   bestWeights[i] = new double[numInputs];
    //   for (int j = 0; j < numInputs; j++) {
    //     bestWeights[i][j] = 0;
    //   }
    // }
    bestWeights = new double[][]{
      {0, 0, 0, 0, 0, 0, 0, 0},
      {0, 0, 0, 0, 0, 0, 0, 0}
    };
    createGeneration();
    setBackground(Color.BLACK);
  }

  public void createGeneration() {
    generation++;
    if (weightRange >= 20) {
      weightRange -= 10;
    }
    else {
      weightRange /= 10;
    }
    rockets[0] = new Rocket(bestWeights);
    // for (int ri = 1; ri < rockets.length; ri++) {
    //   double[][] newWeights = new double[2][numInputs];
    //   for (int i = 0; i < bestWeights.length; i++) {
    //     for (int j = 0; j < bestWeights[0].length; j++) {
    //       newWeights[i][j] = bestWeights[i][j] + Math.random() * weightRange - weightRange / 2;
    //     }
    //   }
    //   rockets[ri] = new Rocket(newWeights);
    // }
    for (int ri = 1; ri < rockets.length; ri++) {
      double[][] newWeights = new double[2][8];
      for (int i = 0; i < bestWeights.length; i++) {
        for (int j = 0; j < bestWeights[0].length; j++) {
          newWeights[i][j] = bestWeights[i][j] + Math.random() * weightRange - weightRange / 2;
        }
      }
      rockets[ri] = new Rocket(newWeights);
    }
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.setColor(Color.GRAY);
    for (Planet p : planets) {
      g.fillOval((int)(p.x - p.radius), (int)(p.y - p.radius),
        (int)(2 * p.radius), (int)(2 * p.radius));
    }
    g.setColor(Color.WHITE);
    for (Rocket r : rockets) {
      g.fillOval((int) r.x, (int) r.y, 5, 5);
    }
    g.drawString("Time: " + String.format("%.2f", time), 5, 20);
    g.drawString("Generation: " + generation, 5, 40);
  }

  class Planet {

    public double x, y, radius, m;

    public Planet(double xIn, double yIn, double rIn, double mIn) {
      x = xIn;
      y = yIn;
      radius = rIn;
      m = mIn;
    }

    public void pull(Rocket r) {
      r.vx += (m * (x - r.x)) /
        Math.pow(Math.pow(x - r.x, 2) +
        Math.pow(y - r.y, 2), 1.5) * dt;
      r.vy += (m * (y - r.y)) /
        Math.pow(Math.pow(x - r.x, 2) +
        Math.pow(y - r.y, 2), 1.5) * dt;
    }

    public boolean crashed(Rocket r) {
      return Math.pow(x - r.x, 2) + Math.pow(y - r.y, 2)
        < Math.pow(radius, 2);
    }

  }

  class Rocket {

    public double x, y, vx, vy, bearing, throttle;
    public boolean isDown;
    public double[] inputs;
    public double[][] weights;

    public Rocket(double[][] weightsIn) {
      x = 600;
      y = 200;
      vx = 0;
      vy = 0;
      bearing = 0;
      throttle = 0;
      isDown = false;
      // inputs = new double[numInputs];
      inputs = new double[8];
      weights = weightsIn;
    }

    public void update() {
      calculateInputs();
      changeTandB();
      vx += throttle * Math.cos(bearing) * dt;
      vy += throttle * Math.sin(bearing) * dt;
      for (Planet p : planets) {
        p.pull(this);
      }
      x += vx * dt;
      y += vy * dt;
      if (hasCrashed())
        isDown = true;
    }

    public boolean hasCrashed() {
      if (x < 0 || x > length|| y < 0 || y > height)
        return true;
      for (Planet p : planets)
        if (p.crashed(this)) return true;
      return false;
    }

    public void calculateInputs() {
      double trueX = x;
      double trueY = y;
      for (int i = 0; i < inputs.length; i++) {
        // double b = bearing + i * 360 / (double) numInputs;
        double b = bearing + i * 45;
        double d = 0;
        while (!hasCrashed()) {
          d += 1;
          x += Math.cos(b);
          y += Math.sin(b);
        }
        inputs[i] = d;
        x = trueX;
        y = trueY;
      }
    }

    public void changeTandB() {
      for (int i = 0; i < weights.length; i++) {
        double sum = 0;
        for (int j = 0; j < inputs.length; j++) {
          sum += inputs[j] * weights[i][j];
        }
        double val = 1.0 / (1.0 + Math.pow(Math.E, -1 * sum));
        if (i == 0) {
          if (val < 0.3) {
            bearing -= 5;
          }
          else if (val > 0.7) {
            bearing += 5;
          }
        }
        else {
          if (val < 0.3) {
            throttle -= 3;
          }
          else if (val > 0.7) {
            throttle += 3;
          }
        }
      }
    }
  }

  class TimerHandler implements ActionListener {

    public void actionPerformed(ActionEvent evt) {
      boolean allDown = true;
      time += dt;
      for (Rocket r : rockets) {
        if (!r.isDown) {
          allDown = false;
          bestWeights = r.weights;
          r.update();
        }
      }
      repaint();
      if (allDown) {
        timer.stop();
        time = 0;
        createGeneration();
        timer.start();
      }
    }

  }

}
