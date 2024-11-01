Real-time Dead-End Detection and Direction Guidance Using OpenCV and Quadtree Decomposition
This project implements a real-time system for detecting dead-ends and providing directional guidance using OpenCV and quadtree decomposition. By analyzing video from a live camera feed, it detects dead ends and suggests movement directions to aid in navigation, making it useful for autonomous systems in dynamic environments.

Features
Real-time Video Processing: Processes video frames in real-time from a live camera feed.
Edge Detection: Uses Canny edge detection to identify edges in each frame.
Quadtree Decomposition: Efficiently segments frames using a quadtree structure.
Dead-End Identification: Detects areas with high edge density and limited connectivity.
Directional Guidance: Provides movement suggestions (left, right, forward, backward) based on path analysis.
Java Swing GUI: Displays the live video feed with detected dead-ends and directional guidance.
Requirements
Java 8 or higher
OpenCV 4.x or higher
OpenCV Java library (available from OpenCV installation)
Ensure that OpenCV is installed and configured correctly on your system. For setup assistance, refer to the OpenCV Java Installation Guide.

Installation
Clone the Repository:

bash
Copy code
git clone https://github.com/ThiccBoiPala/Real-time-Dead-End-Detection-and-Direction-Guidance-Using-OpenCV-and-Quadtree-Decomposition.git
cd Real-time-Dead-End-Detection-and-Direction-Guidance-Using-OpenCV-and-Quadtree-Decomposition
Set Up OpenCV Library in Your Project:

Include OpenCV's opencv_java4xx.dll or .so in your Java library path.
Add OpenCV library files to your project build path.
Compile and Run:

Open the project in your preferred Java IDE (like IntelliJ IDEA or Eclipse).
Compile and run the Camera.java file.
Usage
Start the Application:

Run the Camera.java file to open the GUI.
The application will access the default camera (ID 0) and begin video processing.
Dead-Ends and Direction Guidance:

The GUI displays live video with red circles marking detected dead-ends.
Suggested directions appear in the top left (e.g., "Move Left", "Move Forward", "Move Backward") based on path analysis.
Project Structure
Camera.java: Main class for initializing the camera feed, GUI, and image processing.
Quadtree.java: Implements the quadtree structure for dead-end detection.
OpenCV Functions: Uses various OpenCV functions for edge detection, image segmentation, and display.
Troubleshooting
Ensure the OpenCV library is correctly linked.
Check that camera permissions are enabled.
Adjust edge detection thresholds in processFrame for different lighting conditions.
License
This project is licensed under the MIT License. See the LICENSE file for details.

Acknowledgements
OpenCV for real-time computer vision processing.
Java Swing for GUI components.
Research inspiration on dead-end detection and navigation.
