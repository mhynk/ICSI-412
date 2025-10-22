import java.util.LinkedList;

public class PCB { // Process Control Block
    private static int nextPid = 1;
    public int pid;
    private OS.PriorityType priority;
    private UserlandProcess up; //hold ULP
    private long wakeTime = 0; //wake up!
    public int timeoutCount = 0;
    public int[] deviceIds = new int[10];
    public String name;
    public LinkedList<KernelMessage> messageQueue = new LinkedList<>();
    public boolean isWaitingForMessage = false;

    public PCB(UserlandProcess ulp, int pid) {
        this.pid = pid;
        this.name = ulp.getClass().getSimpleName(); //ping //KLP = Kernel Level Process(PCB)
    }

    PCB(UserlandProcess up, OS.PriorityType priority) {
        this(); //call default constructor -> do deviceIds initiation first
        this.pid = nextPid++; //give Process IDentifier
        this.priority = priority;
        this.up = up;

        this.name = up.getClass().getSimpleName();
        System.out.println("[PCB] Created PCB pid=" + this.pid +
                " type=" + up.getClass().getSimpleName() +
                " priority=" + priority);
    }

    //initiate device Id array, to make it more stable
    public PCB() {
        for (int i = 0; i < deviceIds.length; i++) {
            deviceIds[i] = -1;
        }
    }

    public void incrementTimeout() {
        timeoutCount++;
    }

    public void resetTimeout() {
        timeoutCount = 0;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    public void setWakeTime(long t) {
        wakeTime = t;
    }

    public long getWakeTime() {
        return wakeTime;
    }

    public String getName() {
        return this.name;
    }

    public UserlandProcess getProcess() {
        return up;
    }

    public int getPid() {
        return pid;
    }

    OS.PriorityType getPriority() {
        return priority;
    }

    public void setPriority(OS.PriorityType newPriority) {
        priority = newPriority;
    }

    public void requestStop() {
        up.requestStop(); //call ULP method
    }

    public void stop() { /* calls userlandprocess’ stop. Loops with Thread.sleep() until
ulp.isStopped() is true.  */
        up.stop(); //call ULP stop()
        while (!up.isStopped()) { //call ULP isStopped()
            try{
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isDone() { /* calls userlandprocess’ isDone() */
        return up.isDone(); //call ULP method
    }

    void start() { /* calls userlandprocess’ start() */
        up.start(); //call ULP method
    }
}
