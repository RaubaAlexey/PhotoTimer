package com.example.laptop.phototimer.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.hardware.Camera;


public class CameraUtils {

	public static int numberOfCameras() {
		try {
			Method m = Camera.class.getMethod("getNumberOfCameras");
			return ((Number)m.invoke(null)).intValue();
		}
		catch(Exception ex) {
			return 1;
		}
	}

	@SuppressWarnings("unchecked")
    public static List<Camera.Size> previewSizesForCameraParameters(Camera.Parameters params) {
    	try {
    		Method m = params.getClass().getMethod("getSupportedPreviewSizes");
    		return (List<Camera.Size>)m.invoke(params);
    	}
    	catch(Exception ex) {
    		return null;
    	}
	}

	public static Camera.Size bestCameraSizeForWidthAndHeight(Camera.Parameters params, int width, int height) {
		List<Camera.Size> previewSizes = previewSizesForCameraParameters(params);
		if (previewSizes==null || previewSizes.size()==0) return null;

    	Camera.Size bestSize = null;
    	int bestDiff = 0;
		for(Camera.Size size : previewSizes) {
			int diff = Math.abs(size.width - width) + Math.abs(size.height - height);
			if (bestSize==null || diff<bestDiff) {
				bestSize = size;
				bestDiff = diff;
			}
		}
		return bestSize;
	}

	public static Camera.Size setNearestCameraPreviewSize(Camera camera, int width, int height) {
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = bestCameraSizeForWidthAndHeight(params, width, height);
		if (size!=null) {
			params.setPreviewSize(size.width, size.height);
			camera.setParameters(params);
		}
		return params.getPreviewSize();
	}


	@SuppressWarnings("unchecked")
    public static List<Camera.Size> pictureSizesForCameraParameters(Camera.Parameters params) {
    	try {
    		Method m = params.getClass().getMethod("getSupportedPictureSizes");
    		return (List<Camera.Size>)m.invoke(params);
    	}
    	catch(Exception ex) {
    		return null;
    	}
	}

	public static Camera.Size setLargestCameraSize(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> pictureSizes = pictureSizesForCameraParameters(params);
		if (pictureSizes!=null && pictureSizes.size()>0) {
			long bestPixels = -1;
			Camera.Size bestSize = null;
			for(Camera.Size size : pictureSizes) {
				long pixels = size.width * size.height;
				if (pixels>bestPixels || bestPixels<0) {
					bestPixels = pixels;
					bestSize = size;
				}
			}
			if (bestSize!=null) {
				params.setPictureSize(bestSize.width, bestSize.height);
				camera.setParameters(params);
			}
		}

		return params.getPictureSize();
	}

	public static Bitmap fillGrayscaleBitmapFromCameraData(Bitmap bitmap, byte[] cdata, int width, int height) {
		int[] pixels = new int[cdata.length];
		for(int i=0; i<cdata.length; i++) {
			int g = 0xff & cdata[i];
			pixels[i] = (255<<24) + (g<<16) + (g<<8) + g;
		}
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	public static Camera openCamera(int cameraId) {
		if (cameraId>=0) {
			Method openMethod = null;
			try {
				openMethod = Camera.class.getMethod("open", int.class);
			}
			catch(Exception ex) {
				openMethod = null;
			}
			if (openMethod!=null) {
				try {
					return (Camera)openMethod.invoke(null, cameraId);
				}
				catch(Exception ignored) {}
			}
		}
		return Camera.open();
	}

	static Class<? extends byte[]> BYTE_ARRAY_CLASS = (new byte[0]).getClass();
	static Method addPreviewBufferMethod;
	static Method setPreviewCallbackWithBufferMethod;

	static {
		try {
			addPreviewBufferMethod = Camera.class.getMethod("addCallbackBuffer", BYTE_ARRAY_CLASS);
			setPreviewCallbackWithBufferMethod = Camera.class.getMethod("setPreviewCallbackWithBuffer", Camera.PreviewCallback.class);
		}
		catch(Exception notFound) {
			addPreviewBufferMethod = setPreviewCallbackWithBufferMethod = null;
		}
	}

	public static boolean previewBuffersSupported() {
		return addPreviewBufferMethod!=null;
	}

	public static boolean createPreviewCallbackBuffers(Camera camera, int nbuffers) {
		if (addPreviewBufferMethod==null) return false;

		Camera.Size previewSize = camera.getParameters().getPreviewSize();
		int bufferSize = previewSize.width * previewSize.height * 3 / 2;
		for(int i=0; i<nbuffers; i++) {
			byte[] buffer = new byte[bufferSize];
			try {
				addPreviewBufferMethod.invoke(camera, buffer);
			}
			catch(Exception ignored) {
				return false;
			}
		}
		return true;
	}

	public static boolean addPreviewCallbackBuffer(Camera camera, byte[] buffer) {
		if (addPreviewBufferMethod==null) return false;
		try {
			addPreviewBufferMethod.invoke(camera, buffer);
			return true;
		}
		catch(Exception ignored) {
			return false;
		}
	}

	public static boolean setPreviewCallbackWithBuffer(Camera camera, Camera.PreviewCallback callback) {
		if (setPreviewCallbackWithBufferMethod==null) {
			camera.setPreviewCallback(callback);
			return false;
		}
		try {
			setPreviewCallbackWithBufferMethod.invoke(camera, callback);
			return true;
		}
		catch(Exception ignored) {
			camera.setPreviewCallback(callback);
			return false;
		}
	}

	public static List<String> getFlashModes(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		try {
			Method flashModesMethod = params.getClass().getMethod("getSupportedFlashModes");
			@SuppressWarnings("unchecked")
            List<String> result = (List<String>)flashModesMethod.invoke(params);
			if (result!=null) return result;
		}
		catch(Exception ignored) {}
		return Collections.singletonList("off");
	}

	public static boolean setFlashMode(Camera camera, String mode) {
		Camera.Parameters params = camera.getParameters();
		try {
			Method flashModeMethod = params.getClass().getMethod("setFlashMode", String.class);
			flashModeMethod.invoke(params, mode);
			camera.setParameters(params);
			return true;
		}
		catch(Exception ignored) {
			return false;
		}
	}

	public static boolean cameraSupportsFlash(Camera camera) {
		return getFlashModes(camera).contains("on");
	}

	public static boolean cameraSupportsAutoFlash(Camera camera) {
		return getFlashModes(camera).contains("auto");
	}

    public static class CameraInfo {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;

        public int orientation;
        public int facing;

        public boolean isFrontFacing() {
            return facing == CAMERA_FACING_FRONT;
        }

        public boolean isRotated180Degrees() {
            return (isFrontFacing() && orientation == 90) || (!isFrontFacing() && orientation == 270);
        }
    }

    public static CameraInfo getCameraInfo(int cameraId) {
        try {
            Class<?> cameraInfoClass = Class.forName("android.hardware.Camera$CameraInfo");
            Object cameraInfo = cameraInfoClass.newInstance();
            Method getCameraId = Camera.class.getMethod("getCameraInfo", int.class, cameraInfoClass);
            getCameraId.invoke(null, cameraId, cameraInfo);
            CameraInfo info = new CameraInfo();
            info.facing = cameraInfoClass.getField("facing").getInt(cameraInfo);
            info.orientation = cameraInfoClass.getField("orientation").getInt(cameraInfo);
            return info;
        }
        catch(Exception ex) {
            return new CameraInfo();
        }
    }
}
