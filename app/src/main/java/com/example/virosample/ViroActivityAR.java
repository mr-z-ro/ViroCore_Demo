package com.example.virosample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.viro.core.ARAnchor;
import com.viro.core.ARImageTarget;
import com.viro.core.ARNode;
import com.viro.core.ARScene;
import com.viro.core.AnimationTimingFunction;
import com.viro.core.AnimationTransaction;
import com.viro.core.AsyncObject3DListener;
import com.viro.core.ClickListener;
import com.viro.core.ClickState;
import com.viro.core.Material;
import com.viro.core.Node;

import com.viro.core.Object3D;
import com.viro.core.OmniLight;
import com.viro.core.Spotlight;
import com.viro.core.Surface;
import com.viro.core.Texture;
import com.viro.core.Vector;
import com.viro.core.ViroMediaRecorder;
import com.viro.core.ViroView;
import com.viro.core.ViroViewARCore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ViroActivityAR extends Activity {

    private static final String TAG = ViroActivityAR.class.getSimpleName();
    protected ViroView mViroView;
    private AssetManager mAssetManager;
    private ARScene mARScene;
    private ARImageTarget mImageTarget;
    private Node mGebNode;
    private Object3D mGebModel;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViroView = new ViroViewARCore(this, new ViroViewARCore.StartupListener() {
            @Override
            public void onSuccess() {
                onRenderCreate();
            }

            @Override
            public void onFailure(ViroViewARCore.StartupError error, String errorMessage) {
                // Fail as you wish!
            }
        });
        mViroView.setPBREnabled(true);
        setContentView(mViroView);
    }

    private void onRenderCreate() {
        // Create the base ARScene
        mARScene = new ARScene();

        // Create an ARImageTarget out of the GEB cover
        Bitmap bookCover = bitmapFromAsset("geb.jpg");
        mImageTarget = new ARImageTarget(bookCover, ARImageTarget.Orientation.Up, 0.075f);
        mARScene.addARImageTarget(mImageTarget);

        // Create a Node containing the GEB model
        mGebNode = loadGebNode();
        mGebNode.addChildNode(initLightingNode());
        mARScene.getRootNode().addChildNode(mGebNode);

        mViroView.setScene(mARScene);
        trackImageNodeTargets();
    }

    private void trackImageNodeTargets() {
        // Create an ARScene.Listener to listen for the onTrackingInitialized() callback. This
        // callback tells us when the AR system is ready to go
        mARScene.setListener(new ARScene.Listener() {
            @Override
            public void onTrackingInitialized() {
                // No-op
            }

            @Override
            public void onTrackingUpdated(ARScene.TrackingState trackingState, ARScene.TrackingStateReason trackingStateReason) {
                // No-op
            }

            @Override
            public void onAmbientLightUpdate(float v, Vector vector) {
                // No-op
            }

            @Override
            public void onAnchorFound(ARAnchor arAnchor, ARNode arNode) {
                Log.i(TAG, "Target Found!!!");

                // Ensure the image target Viro found is our poster
                String anchorId = arAnchor.getAnchorId();
                if (!mImageTarget.getId().equalsIgnoreCase(anchorId)) {
                    return;
                }

                // Optionally set the model position relative to the image
                mGebNode.setPosition(arAnchor.getPosition());
                mGebNode.setRotation(arAnchor.getRotation());
                mGebModel.setVisible(true);
            }

            @Override
            public void onAnchorUpdated(ARAnchor arAnchor, ARNode arNode) {
                // No-op
            }

            @Override
            public void onAnchorRemoved(ARAnchor arAnchor, ARNode arNode) {
                String anchorId = arAnchor.getAnchorId();
                if (!mImageTarget.getId().equalsIgnoreCase(anchorId)) {
                    return;
                }

                mGebNode.setVisible(false);
            }
        });
    }

    private Node loadGebNode() {
        Node node = new Node();

        mGebModel = new Object3D();
        final Bitmap wood = bitmapFromAsset("wood.jpg");
        mGebModel.loadModel(mViroView.getViroContext(), Uri.parse("file:///android_asset/geb.obj"), Object3D.Type.OBJ, new AsyncObject3DListener() {
            public void onObject3DFailed(String error) {
                Log.w(TAG, "Failed to load the model");
            }
            public void onObject3DLoaded(Object3D object, Object3D.Type type) {
                Log.i(TAG, "Successfully loaded the model!");
                // When the model is loaded, set the texture associated with this OBJ.
                Texture objectTexture = new Texture(wood, Texture.Format.RGBA8, false, false);
                Material material = new Material();
                material.setLightingModel(Material.LightingModel.PHYSICALLY_BASED);
                material.setMetalness(0.25f);
                material.setDiffuseTexture(objectTexture);
                mGebModel.getGeometry().setMaterials(Arrays.asList(material));
            }
        });

        mGebModel.setVisible(false);
        node.addChildNode(mGebModel);

        node.setClickListener(new ClickListener() {

            @Override
            public void onClick(int i, Node node, Vector vector) {
                if (!hasAudioAndRecordingPermissions(getBaseContext())){
                    requestPermissions();
                    return;
                }
                //toggleRecording();
                AnimationTransaction.begin();
                AnimationTransaction.setAnimationDuration(2000);
                AnimationTransaction.setTimingFunction(AnimationTimingFunction.EaseInEaseOut);
                Vector currentRotation = node.getRotationEulerRealtime();
                node.setRotation(new Vector(currentRotation.x, currentRotation.y + Math.PI, currentRotation.z));
                AnimationTransaction.commit();
            }

            @Override
            public void onClickState(int i, Node node, ClickState clickState, Vector vector) {

            }
        });

        return node;
    }

    private void toggleRecording() {
        if (!isRecording) {
            mViroView.getRecorder().startRecordingAsync("virotest", true, new ViroMediaRecorder.RecordingErrorListener() {
                @Override
                public void onRecordingFailed(ViroMediaRecorder.Error error) {
                    Log.e(TAG, error.toString());
                }
            });
            isRecording = true;
        } else {
            mViroView.getRecorder().stopRecordingAsync(new ViroMediaRecorder.VideoRecordingFinishListener() {
                @Override
                public void onSuccess(String s) {
                    Log.i(TAG, "File recorded to: " + s);
                }

                @Override
                public void onError(ViroMediaRecorder.Error error) {
                    Log.e(TAG, error.toString());
                }
            });
            isRecording = false;
        }
    }

    private Node initLightingNode() {
        Vector omniLightPositions [] = {    new Vector(-3, 3, 0.3),
                new Vector(3, 3, 1),
                new Vector(-3,-3,1),
                new Vector(3, -3, 1)};

        Node lightingNode = new Node();
        for (Vector pos : omniLightPositions){
            final OmniLight light = new OmniLight();
            light.setPosition(pos);
            light.setColor(Color.parseColor("#FFFFFF"));
            light.setIntensity(5);
            light.setAttenuationStartDistance(6);
            light.setAttenuationEndDistance(9);

            lightingNode.addLight(light);
        }

        // The spotlight will cast the shadows
        Spotlight spotLight = new Spotlight();
        spotLight.setPosition(new Vector(0,5,-0.5));
        spotLight.setColor(Color.parseColor("#FFFFFF"));
        spotLight.setDirection(new Vector(0, -1, 0));
        spotLight.setIntensity(10);
        spotLight.setShadowOpacity(0.4f);
        spotLight.setShadowMapSize(2048);
        spotLight.setShadowNearZ(2f);
        spotLight.setShadowFarZ(7f);
        spotLight.setInnerAngle(5);
        spotLight.setOuterAngle(20);
        spotLight.setCastsShadow(true);
        lightingNode.addLight(spotLight);

        // Add shadow planes: these are "invisible" surfaces on which virtual shadows will be cast,
        // simulating real-world shadows
        final Material material = new Material();
        material.setShadowMode(Material.ShadowMode.TRANSPARENT);

        Surface surface = new Surface(3, 3);
        surface.setMaterials(Arrays.asList(material));

        Node surfaceShadowNode = new Node();
        surfaceShadowNode.setRotation(new Vector(Math.toRadians(-90), 0, 0));
        surfaceShadowNode.setGeometry(surface);
        surfaceShadowNode.setPosition(new Vector(0, 0, 0.0));
        lightingNode.addChildNode(surfaceShadowNode);

        lightingNode.setRotation(new Vector(Math.toRadians(-90), 0, 0));
        return lightingNode;
    }

    private Bitmap bitmapFromAsset(String assetName) {
        if (mAssetManager == null) {
            mAssetManager = getResources().getAssets();
        }

        InputStream imageStream;
        try {
            imageStream = mAssetManager.open(assetName);
        } catch (IOException exception) {
            Log.w("Viro", "Unable to find image [" + assetName + "] in assets! Error: "
                    + exception.getMessage());
            return null;
        }
        return BitmapFactory.decodeStream(imageStream);
    }

    // You can use the ARSceneListener to respond to AR events, including the detection of
    // anchors
    private class SampleARSceneListener implements ARScene.Listener {
        private Runnable mOnTrackingInitializedRunnable;
        private boolean mInitialized;
        public SampleARSceneListener(Runnable onTrackingInitializedRunnable) {
            mOnTrackingInitializedRunnable = onTrackingInitializedRunnable;
            mInitialized = false;
        }

        @Override
        public void onTrackingUpdated(ARScene.TrackingState trackingState,
                                      ARScene.TrackingStateReason trackingStateReason) {
            if (trackingState == ARScene.TrackingState.NORMAL && !mInitialized) {
                mInitialized = true;
                if (mOnTrackingInitializedRunnable != null) {
                    mOnTrackingInitializedRunnable.run();
                }
            }
        }

        @Override
        public void onTrackingInitialized() {
            // this method is deprecated.
        }

        @Override
        public void onAmbientLightUpdate(float lightIntensity, Vector color) {
            // no-op
        }

        @Override
        public void onAnchorFound(ARAnchor anchor, ARNode arNode) {
            // no-op
        }

        @Override
        public void onAnchorUpdated(ARAnchor anchor, ARNode arNode) {
            // no-op
        }

        @Override
        public void onAnchorRemoved(ARAnchor anchor, ARNode arNode) {
            // no-op
        }
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0);
    }

    private static boolean hasAudioAndRecordingPermissions(Context context) {
        boolean hasRecordPermissions = ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean hasExternalStoragePerm = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return hasRecordPermissions && hasExternalStoragePerm;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (!hasAudioAndRecordingPermissions(ViroActivityAR.this)) {
            Toast toast = Toast.makeText(ViroActivityAR.this, "User denied permissions", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViroView.onActivityStarted(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViroView.onActivityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViroView.onActivityPaused(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mViroView.onActivityStopped(this);
    }
}