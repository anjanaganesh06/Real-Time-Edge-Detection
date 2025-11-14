# Real-Time Edge Detection Viewer  
**Android + OpenCV (C++) + OpenGL ES + TypeScript Web Viewer**

A lightweight R&D demo that processes live camera frames on Android using  
OpenCV (C++ via JNI), renders them with OpenGL ES, and includes a TypeScript  
web viewer that displays a sample processed frame.

---

## 🚀 Features Implemented

### 📱 Android (Native + OpenGL)
- Live camera capture (TextureView)
- JNI bridge to C++ for frame processing
- OpenCV Canny / Grayscale filters (toggle support)
- NV21 → RGBA conversion in C++
- Real-time rendering using OpenGL ES 2.0
- ByteBuffer-based GPU texture upload
- Optional frame saving (`processed_frame.png`) for web viewer

### 🧩 C++ (JNI + OpenCV)
- Native function `processFrameNV21(...)`
- OpenCV Canny/Gray edge detection
- Runs per-frame on a direct RGBA buffer

### 🎨 OpenGL ES Renderer
- GLSurfaceView + custom MyGLRenderer
- Fragment shader for displaying RGBA texture
- Efficient ByteBuffer texture uploads

### 🌐 TypeScript Web Viewer
- Simple static viewer that loads the exported processed frame
- Displays FPS + resolution (browser simulated FPS)
- Built with TypeScript → ES6

---

## 🛠️ Project Structure
