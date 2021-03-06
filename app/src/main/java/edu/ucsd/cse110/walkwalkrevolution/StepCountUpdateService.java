package edu.ucsd.cse110.walkwalkrevolution;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class StepCountUpdateService extends Service {

    public static final String TAG = "StepCountUpdateService";

    public static final String BROADCAST_ACTION = "edu.ucsd.cse110.STEP_COUNT_UPDATE";

    private final IBinder binder = new LocalBinder();
    private boolean isRunning;
    private int interval;

    public StepCountUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class LocalBinder extends Binder {
        public StepCountUpdateService getService() {
            return StepCountUpdateService.this;
        }
    }

    // Exposed method to be able to stop service when service is bound
    // Service cannot be restarted, a new service has to be created
    public void stop() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting step count update service");
        interval = intent.getIntExtra("interval", 1000);
        Thread thread = new Thread(new MyThread(startId));
        thread.start();
        isRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    final class MyThread implements Runnable {
        int startId;
        public MyThread(int startId) {
            this.startId = startId;
        }

        @Override
        public void run() {
            Intent broadcastIntent = new Intent(BROADCAST_ACTION);
            synchronized (this) {
                while (isRunning) {
                    try {
                        sendBroadcast(broadcastIntent);
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
