import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{
    private Thread thread;
    private Semaphore semaphore = new Semaphore(0);
    private boolean quantumExpired;

    public Process() {
        thread = new Thread(this);
        thread.start();
    }

    public void requestStop() {
            quantumExpired = true;
    }

    public abstract void main();

    public boolean isStopped() {
        if (semaphore == null) {
            return false;
        }
        return semaphore.availablePermits() == 0;
    }

    public boolean isDone() {
        if (thread == null) {
            return false;
        }
        return !thread.isAlive();
    }

    public void start() {
        semaphore.release();

    }

    public void stop() {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }
    //release the semaphore

    public void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            semaphore.acquire(); //stop first
            main();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public void cooperate() {
        if (quantumExpired) {
        quantumExpired = false;
        OS.switchProcess();
        }
    }
}
