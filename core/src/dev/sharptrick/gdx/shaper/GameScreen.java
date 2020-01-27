package dev.sharptrick.gdx.shaper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;

import java.util.Arrays;

public class GameScreen implements Screen {
    public static final int GESTURE_DIMENSIONS = 4;
    public static final int GESTURE_RESOLUTION = 1024;

    private static float CLEAR_TIME = 1;

    private final Shaper game;
    private final OrthographicCamera cam;
    //private final PerspectiveCamera cam;

    private int width, height;
    private float maxStrokeWidth;

    private float r = 1, g = 1, b = 1, a = 1;
    private float time = 0;


    private boolean autoClear = true;
    private boolean gestureInitiated = false;

    private int gestureCount = 0;
    private int currentResolution = GESTURE_RESOLUTION;

    private float lastTime = 0;

    private float minPressure = 1;
    private float maxPressure = 0;

    private int xIdx = 0;
    private int yIdx = 1;
    private int timeIdx = 2;
    private int pressureIdx = 3;

    private final float[][] renderBuffer = new float[GESTURE_DIMENSIONS][GESTURE_RESOLUTION + 1];
    private final float[][] targetBuffer = new float[GESTURE_DIMENSIONS][GESTURE_RESOLUTION + 1];

    private final Color c0 = new Color();
    private final Color c1 = new Color();
    private final float[] v0 = new float[GESTURE_DIMENSIONS];
    private final float[] v1 = new float[GESTURE_DIMENSIONS];
    private final float[][] swapBuffer = new float[GESTURE_DIMENSIONS][];

    private final Vector3 inputPos = new Vector3();
    //    private final Array<Vector2> pathArray = new Array<Vector2>(3);
    private final float[] polygonVertices = new float[8];
    private final Polygon polygon = new Polygon(polygonVertices);

    public GameScreen(final Shaper game) {
        this.game = game;
//        for (int i = 0; i < PATH_LENGTH; i++) pathArray.add(new Vector2());

        cam = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getWidth());

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        // process user input
        if (Gdx.input.isTouched() || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            inputPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            cam.unproject(inputPos);
            processInput();
        } else {
            gestureInitiated = false;
        }


        // clear the screen with a dark blue color. The
        // arguments to glClearColor are the red, green
        // blue and alpha component in the range [0,1]
        // of the color to be used to clear the screen.
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // tell the camera to update its matrices.
        cam.update();
        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        game.batch.setProjectionMatrix(cam.combined);
        game.shapeDrawer.update();

        //render
        game.batch.begin();
        drawGestures();
        game.batch.end();


        // perform game logic

    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        maxStrokeWidth = Math.min(width, height) / 20;
        cam.setToOrtho(false, width, height);
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
    }

    private void setRainbowColor(float rad) {
        r = MathUtils.cos(rad / 2);
        r = r * r;

        g = MathUtils.cos(rad / 2f + MathUtils.PI / 3);
        g = g * g;

        b = MathUtils.cos(rad / 2f + MathUtils.PI2 / 3);
        b = b * b;

    }

    public void clearGesture() {
        gestureCount = 0;
        currentResolution = GESTURE_RESOLUTION;

        minPressure = 1;
        maxPressure = 0;
    }

    private void drawGestures() {
        if (0 < currentResolution) {

//            long current = System.currentTimeMillis();
//            long initialDeltaTime = current - initialTime;
            float cycleValue = time % 1f;
            //float prevOmega = 0;
            //float prevOmegaAvg = 0;

            //draw each gesture
            for (int gestureIdx = 0; gestureIdx < gestureCount; gestureIdx++) {
                int startIndex = gestureIdx * currentResolution;
                int endIndex = startIndex + currentResolution;

                //initialize v0
                for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
                    v0[z] = renderBuffer[z][startIndex];
                }
                v0[xIdx] = Math.round(v0[xIdx] * width);
                v0[yIdx] = Math.round(v0[yIdx] * height);

                float deltaTime = time - v0[timeIdx];
                float rad = MathUtils.PI2 * (deltaTime % 1f);
                setRainbowColor(rad);
                c0.set(r, g, b, a);
                game.shapeDrawer.setColor(c0);

                float strokeWidth = 0;

                //draw each line segment
                for (int i = startIndex; i < endIndex; i++) {
                    //calculate v1 as the cyclic midpoint for smooth aesthetics
                    for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
                        v1[z] = renderBuffer[z][i];
                        if (i < endIndex - 1) {
                            v1[z] += cycleValue * (renderBuffer[z][i + 1] - renderBuffer[z][i]);
                        }
                    }
                    v1[xIdx] = Math.round(v1[xIdx] * width);
                    v1[yIdx] = Math.round(v1[yIdx] * height);

                    //cycle color based on touch time
                    deltaTime = time - v1[timeIdx];
                    rad = MathUtils.PI2 * (deltaTime % 1f);
                    setRainbowColor(rad);
                    c1.set(r, g, b, a);

                    //use relative pressure and time since touch to calculate width
                    float pressure = maxPressure == minPressure ? 1 : (v1[pressureIdx] - minPressure) / (maxPressure - minPressure);
                    strokeWidth = maxStrokeWidth * pressure * (-1 / (deltaTime + 1) + 1);

                    //float omega = MathUtils.atan2(v1[yIdx] - v0[yIdx], v1[xIdx] - v0[xIdx]);

//                    if (i == startIndex) {
//                        game.shapeDrawer.setColor(c0);
//                        //prevOmega = omega;
//                        game.shapeDrawer.filledCircle(
//                            width * v0[xIdx], height * v0[yIdx],
//                            strokeWidth / 2
//                        );
//                    }

                    if (v0[xIdx] != v1[xIdx] || v0[yIdx] != v1[yIdx]) {
                        game.shapeDrawer.filledCircle(
                            v0[xIdx], v0[yIdx],
                            strokeWidth / 2
                        );

                        game.shapeDrawer.line(
                            v0[xIdx], v0[yIdx],
                            v1[xIdx], v1[yIdx],
                            strokeWidth
                        );



//                        polygonVertices[0] = width * v0[xIdx] - MathUtils.sin(-prevOmega) * strokeWidth / 2;
//                        polygonVertices[1] = height * v0[yIdx] - MathUtils.cos(-prevOmega) * strokeWidth / 2;
//                        polygonVertices[2] = width * v0[xIdx] + MathUtils.sin(-prevOmega) * strokeWidth / 2;
//                        polygonVertices[3] = height * v0[yIdx] + MathUtils.cos(-prevOmega) * strokeWidth / 2;
//
//                        polygonVertices[4] = width * v1[xIdx] + MathUtils.sin(-omega) * strokeWidth / 2;
//                        polygonVertices[5] = height * v1[yIdx] + MathUtils.cos(-omega) * strokeWidth / 2;
//                        polygonVertices[6] = width * v1[xIdx] - MathUtils.sin(-omega) * strokeWidth / 2;
//                        polygonVertices[7] = height * v1[yIdx] - MathUtils.cos(-omega) * strokeWidth / 2;
//
//                        game.shapeDrawer.filledTriangle(
//                            polygonVertices[0], polygonVertices[1],
//                            polygonVertices[2], polygonVertices[3],
//                            polygonVertices[4], polygonVertices[5],
//                            c0, c0, c1
//                        );
//
//                        game.shapeDrawer.filledTriangle(
//                            polygonVertices[0], polygonVertices[1],
//                            polygonVertices[4], polygonVertices[5],
//                            polygonVertices[6], polygonVertices[7],
//                            c0, c1, c1
//                        );
//                    }

//                    if (i == endIndex - 1) {
//                        game.shapeDrawer.setColor(c1);
//                        game.shapeDrawer.sector(
//                            width * v1[0], height * v1[yIdx],
//                            strokeWidth / 2,
//                            omega - MathUtils.PI / 2, MathUtils.PI
//                        );
//                    }

                        //copy v1 into v0
                        System.arraycopy(v1, 0, v0, 0, GESTURE_DIMENSIONS);
                        c0.set(c1);
                        game.shapeDrawer.setColor(c0);
                    }
                    if(i == endIndex -1){
                        game.shapeDrawer.filledCircle(
                            v1[xIdx], v1[yIdx],
                            strokeWidth / 2
                        );
                    }
                    //prevOmega = omega;
                }
            }
        }
    }


    private void initGesture(float inputX, float inputY, float touchPressure) {

        gestureInitiated = true;
        if (autoClear && 0 < gestureCount && CLEAR_TIME < time - lastTime) clearGesture();

        if (gestureCount < GESTURE_RESOLUTION) {
            if (0 < gestureCount) compressAllGestures();

            // new gesture
            if (gestureCount == 0) {
                lastTime = 0;
                time = 0;
                minPressure = touchPressure;
                maxPressure = touchPressure;
            }
            gestureCount++;

            int startIdx = (gestureCount - 1) * currentResolution;
            int endIdx = startIdx + currentResolution;
            Arrays.fill(renderBuffer[xIdx], startIdx, endIdx, inputX);
            Arrays.fill(renderBuffer[yIdx], startIdx, endIdx, inputY);
            Arrays.fill(renderBuffer[timeIdx], startIdx, endIdx, time);
            Arrays.fill(renderBuffer[pressureIdx], startIdx, endIdx, touchPressure);
        }

    }

    private void processInput() {

        float inputX = inputPos.x / width;
        float inputY = inputPos.y / height;
        float inputPressure = Gdx.input.getPressure();

        int startIdx;
        int endIdx;

        if (!gestureInitiated) initGesture(inputX, inputY, inputPressure);

        if (inputPressure < minPressure) minPressure = inputPressure;
        if (maxPressure < inputPressure) maxPressure = inputPressure;

        startIdx = (gestureCount - 1) * currentResolution;
        endIdx = startIdx + currentResolution;

        renderBuffer[xIdx][endIdx] = inputX;
        renderBuffer[yIdx][endIdx] = inputY;
        renderBuffer[timeIdx][endIdx] = time;
        renderBuffer[pressureIdx][endIdx] = inputPressure;
        lastTime = time;

        extrapolatePoints(startIdx, endIdx + 1, startIdx, endIdx);
        for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
            System.arraycopy(
                targetBuffer[z], startIdx,
                renderBuffer[z], startIdx,
                currentResolution
            );
        }

    }

    private void extrapolatePoints(int inputStartIdx, int inputEndIdx, int targetStartIdx,
                                   int targetEndIdx) {

        //if no time has passed between first and last point, just copy first point
        if (renderBuffer[timeIdx][inputStartIdx] == renderBuffer[timeIdx][inputEndIdx - 1]) {
            for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
                Arrays.fill(targetBuffer[z], targetStartIdx, targetEndIdx, renderBuffer[z][inputStartIdx]);
            }

            //if target resolution is the same as input resolution, just copy array
        } else if (inputEndIdx - inputStartIdx == targetEndIdx - targetStartIdx) {
            for (int z = 0; z < GESTURE_DIMENSIONS; z++)
                System.arraycopy(
                    renderBuffer[z], inputStartIdx,
                    targetBuffer[z], targetStartIdx,
                    targetEndIdx - targetStartIdx
                );
        } else {

            int targetResolution = targetEndIdx - targetStartIdx;
            int a = inputStartIdx;
            int b = a + 1;
            for (int j = 0; j < targetResolution; j++) {
                //target time based on equidistant points in time
                float timeTarget = renderBuffer[timeIdx][inputStartIdx] + j
                    * (renderBuffer[timeIdx][inputEndIdx - 1] - renderBuffer[timeIdx][inputStartIdx])
                    / (targetResolution - 1);

                //find points a & b that straddle target time
                int b0 = b;
                while (b < inputEndIdx - 1 && renderBuffer[timeIdx][b] <= timeTarget) {
                    if (b == b0) a = b;
                    b += 1;
                }

                //perform time based extrapolation
                float timeD = (timeTarget - renderBuffer[timeIdx][a]) / (renderBuffer[timeIdx][b] - renderBuffer[timeIdx][a]);
                for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
                    targetBuffer[z][targetStartIdx + j] = renderBuffer[z][a] + timeD * (renderBuffer[z][b] - renderBuffer[z][a]);
                }
            }
        }
    }

    private void swapBuffers() {
        for (int z = 0; z < GESTURE_DIMENSIONS; z++) {
            swapBuffer[z] = renderBuffer[z];
            renderBuffer[z] = targetBuffer[z];
            targetBuffer[z] = swapBuffer[z];
        }
    }

    private void compressAllGestures() {
        int newGestureResolution = GESTURE_RESOLUTION / (gestureCount + 1);
        if (currentResolution != newGestureResolution) {
            for (int g = 0; g < gestureCount; g++) {
                int currentStartIdx = g * currentResolution;
                int currentEndIdx = currentStartIdx + currentResolution;

                int newStartIdx = g * newGestureResolution;
                int newEndInx = newStartIdx + newGestureResolution;

                extrapolatePoints(currentStartIdx, currentEndIdx, newStartIdx, newEndInx);
            }
            swapBuffers();
            currentResolution = newGestureResolution;
        }
    }


}
