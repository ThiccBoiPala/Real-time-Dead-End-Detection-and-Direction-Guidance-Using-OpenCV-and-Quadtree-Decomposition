package com.opencvcamera;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class Camera extends JFrame {

    private JLabel cameraScreen;
    private VideoCapture capture;
    private Mat image;

    public Camera() {
        GUI();
    }

    private void GUI() {
        setLayout(new BorderLayout());

        cameraScreen = new JLabel();
        add(cameraScreen, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                releaseResources();
            }
        });

        setSize(new Dimension(640, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void releaseResources() {
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        if (image != null) {
            image.release();
        }
    }

    private void displayImage(Mat image) {
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, buf);
        byte[] imageData = buf.toArray();
        ImageIcon icon = new ImageIcon(imageData);
        cameraScreen.setIcon(icon);
    }

    private void processFrame(Mat frame) {
        Mat grayImage = new Mat();
        Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Apply Canny edge detection
        Mat edges = new Mat();
        Imgproc.Canny(grayImage, edges, 50, 150); // Adjust threshold values as needed

        // Construct quadtree
        Quadtree quadtree = new Quadtree(edges, new Rect(0, 0, edges.cols(), edges.rows()));

        // Identify dead ends based on path connectivity
        List<Point> deadEnds = quadtree.findDeadEnds();

        // Draw circles at dead ends
        drawCircles(frame, deadEnds);

        // Determine movement direction
        String direction = determineDirection(edges);

        // Display the processed image with direction
        Imgproc.putText(frame, direction, new Point(10, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
        displayImage(frame);
    }

    private void drawCircles(Mat image, List<Point> points) {
        Scalar color = new Scalar(0, 0, 255); // Red color
        int radius = 3;
        for (Point point : points) {
            Imgproc.circle(image, point, radius, color, -1);
        }
    }

    private String determineDirection(Mat edges) {
        int cols = edges.cols();
        int rows = edges.rows();
        int regionWidth = cols / 3;
        int regionHeight = rows / 3;

        Rect leftRegion = new Rect(0, 0, regionWidth, rows);
        Rect centerRegion = new Rect(regionWidth, 0, regionWidth, rows);
        Rect rightRegion = new Rect(2 * regionWidth, 0, regionWidth, rows);

        int leftEdges = Core.countNonZero(new Mat(edges, leftRegion));
        int centerEdges = Core.countNonZero(new Mat(edges, centerRegion));
        int rightEdges = Core.countNonZero(new Mat(edges, rightRegion));

        // Determine the direction based on fewer edges
        String direction = "Move Forward";
        if (leftEdges < centerEdges && leftEdges < rightEdges) {
            direction = "Move Left";
        } else if (rightEdges < centerEdges && rightEdges < leftEdges) {
            direction = "Move Right";
        } else if (centerEdges < leftEdges && centerEdges < rightEdges) {
            direction = "Move Forward";
        } else {
            direction = "Halt";
        }

        // Check for dead ends in the center region
        if (centerEdges > leftEdges && centerEdges > rightEdges) {
            direction = "Move Backward";
        }

        return direction;
    }

    public void startCamera() {
        capture = new VideoCapture(0);
        image = new Mat();
        byte[] imageData;
        ImageIcon icon;

        while (capture.isOpened()) {
            capture.read(image);
            if (!image.empty()) {
                processFrame(image);
            }

            try {
                Thread.sleep(33); // Approx. 30 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Run GUI-related code in the event-dispatching thread
        EventQueue.invokeLater(() -> {
            Camera camera = new Camera();
            new Thread(camera::startCamera).start();
        });
    }
}

class Quadtree {
    private Node root;
    private static final int MIN_NODE_SIZE = 10; // Minimum size for quadtree nodes
    private Mat edges;

    public Quadtree(Mat edges, Rect region) {
        this.edges = edges;
        this.root = constructQuadtree(region);
    }

    private Node constructQuadtree(Rect region) {
        if (region.width <= MIN_NODE_SIZE || region.height <= MIN_NODE_SIZE) {
            // Terminate subdivision when node size is less than or equal to MIN_NODE_SIZE
            return new Node(region);
        }

        // Check if the region contains any edge pixel
        boolean containsEdge = containsEdge(region);
        if (!containsEdge) {
            // Terminate subdivision if the region does not contain any edge pixel
            return new Node(region);
        }

        // Subdivide the region into quadrants
        int centerX = region.x + region.width / 2;
        int centerY = region.y + region.height / 2;

        Rect topLeft = new Rect(region.x, region.y, region.width / 2, region.height / 2);
        Rect topRight = new Rect(centerX, region.y, region.width / 2, region.height / 2);
        Rect bottomLeft = new Rect(region.x, centerY, region.width / 2, region.height / 2);
        Rect bottomRight = new Rect(centerX, centerY, region.width / 2, region.height / 2);

        Node node = new Node(region);
        node.children.add(constructQuadtree(topLeft));
        node.children.add(constructQuadtree(topRight));
        node.children.add(constructQuadtree(bottomLeft));
        node.children.add(constructQuadtree(bottomRight));

        return node;
    }

    private boolean containsEdge(Rect region) {
        Mat subImage = new Mat(edges, region);
        return Core.countNonZero(subImage) > 0;
    }

    public List<Point> findDeadEnds() {
        List<Point> deadEnds = new ArrayList<>();
        findDeadEnds(root, deadEnds);
        return deadEnds;
    }

    private void findDeadEnds(Node node, List<Point> deadEnds) {
        if (node.children.isEmpty()) {
            // Leaf node
            if (isDeadEnd(node)) {
                // If the node is a dead end, add its center point to the list of dead ends
                deadEnds.add(new Point(node.region.x + node.region.width / 2, node.region.y + node.region.height / 2));
            }
        } else {
            // Internal node
            for (Node child : node.children) {
                findDeadEnds(child, deadEnds); 
            }
        }
    }

    private boolean isDeadEnd(Node node) {
        // Check path connectivity
        Mat subImage = new Mat(edges, node.region);
        int edgeCount = Core.countNonZero(subImage);
        double edgeDensity = (double) edgeCount / (node.region.width * node.region.height);

        // Check if the node is a terminating path
        int intersectionPoints = countEdgeIntersections(node.region);

        // Define thresholds for edge density and intersection points
        double edgeDensityThreshold = 0.05; // Adjust as needed
        int intersectionPointsThreshold = 1; // Adjust as needed

        boolean isDeadEnd = edgeDensity > edgeDensityThreshold && intersectionPoints <= intersectionPointsThreshold;
        
        // Debug output
        System.out.println("Region: " + node.region + " Edge Density: " + edgeDensity + " Intersections: " + intersectionPoints + " Is Dead End: " + isDeadEnd);
        
        return isDeadEnd;
    }

    private int countEdgeIntersections(Rect region) {
        // Improved method to count edge intersection points within the region
        int count = 0;
        for (int y = region.y; y < region.y + region.height; y++) {
            for (int x = region.x; x < region.x + region.width; x++) {
                if (edges.get(y, x)[0] > 0) {
                    // Check neighbors to determine if it's an intersection
                    int neighborCount = 0;
                    if (x > 0 && edges.get(y, x - 1)[0] > 0) neighborCount++;
                    if (x < edges.cols() - 1 && edges.get(y, x + 1)[0] > 0) neighborCount++;
                    if (y > 0 && edges.get(y - 1, x)[0] > 0) neighborCount++;
                    if (y < edges.rows() - 1 && edges.get(y + 1, x)[0] > 0) neighborCount++;
                    if (neighborCount > 2) count++;
                }
            }
        }
        return count;
    }

    private static class Node {
        Rect region;
        List<Node> children;

        public Node(Rect region) {
            this.region = region;
            this.children = new ArrayList<>();
        }
    }
}
