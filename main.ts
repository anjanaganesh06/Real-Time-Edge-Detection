// Dummy processed frame (replace with a real processed frame later)
const SAMPLE_FRAME = "./sample.png";
// You can replace with: "./sample.png"

const frameImg = document.getElementById("frame") as HTMLImageElement;
const fpsText = document.getElementById("fps") as HTMLElement;
const resText = document.getElementById("resolution") as HTMLElement;

// Load sample frame
frameImg.src = SAMPLE_FRAME;

// When image loads → update resolution
frameImg.onload = () => {
    resText.textContent = `${frameImg.naturalWidth} x ${frameImg.naturalHeight}`;
};

// --- FPS Simulation (optional demo) ---
let lastTime = performance.now();
let frameCount = 0;

function updateFPS() {
    const now = performance.now();
    frameCount++;

    if (now - lastTime >= 1000) {
        fpsText.textContent = frameCount.toString();
        frameCount = 0;
        lastTime = now;
    }

    requestAnimationFrame(updateFPS);
}

updateFPS();
