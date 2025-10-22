import java.util.HashMap;

public class Kernel extends Process implements Device {
    private Scheduler scheduler;
    private VFS vfs = new VFS(); //all device-related calls goes to VFS
    private HashMap<Integer, PCB> pidTable = new HashMap<>();
    private int nextPid = 1;
    private PCB currentProcess;

    public Kernel() {
        super();
        scheduler = new Scheduler();
    }

    //accessor!
    public Scheduler getScheduler() {
        return scheduler;
    }

    public PCB getPCB() {
        return scheduler.getCurrentlyRunning();
    }

    @Override
    public void main() {
        while (true) { // Warning on infinite loop is OK...
            System.out.println("Kernel loop tick, currentCall=" + OS.currentCall);

            switch (OS.currentCall) { // get a job from OS, do it
                case CreateProcess ->  // Note how we get parameters from OS and set the return value
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                case SwitchProcess -> scheduler.SwitchProcess();

                // Priority Scheduler
                case Sleep -> Sleep((int) OS.parameters.get(0));
                case GetPID -> OS.retVal = GetPid();
                case Exit -> {
                    Exit();
                    OS.retVal = 0;
                }

                // Devices
                case Open -> OS.retVal = Open((String) OS.parameters.get(0));
                case Close -> Close((int) OS.parameters.get(0));
                case Read -> OS.retVal = Read((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                case Seek -> Seek((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                case Write -> OS.retVal = Write((int) OS.parameters.get(0), (byte[]) OS.parameters.get(1));

                    /*
                    // Messages
                    case GetPIDByName ->
                    case SendMessage ->
                    case WaitForMessage ->
                    // Memory
                    case GetMapping ->
                    case AllocateMemory ->
                    case FreeMemory ->
                     */
            }
            // TODO: Now that we have done the work asked of us, start some process then go to sleep.
                /*if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.start();
                }*/
                /*if (scheduler.currentlyRunning != null && !scheduler.currentlyRunning.isDone()) {
                   scheduler.currentlyRunning.start();
                }*/
            OS.currentCall = null;
            scheduler.currentlyRunning.start();

            this.stop();
        }
    }

    private void Sleep(int mills) {
        scheduler.Sleep(mills);
    }

    private void Exit() {
        PCB pcb = scheduler.getCurrentlyRunning();
        closeAll(pcb);
        scheduler.exitCurrentProcess();
        scheduler.SwitchProcess();
    }

    private int GetPid() {
        if (scheduler.currentlyRunning != null) {
            return scheduler.currentlyRunning.getPid();
        }
        return -1;
    }

    @Override
    public int Open(String s) {
        PCB pcb = scheduler.getCurrentlyRunning();
        for (int i = 0; i < pcb.deviceIds.length; i++) {
            if (pcb.deviceIds[i] == -1) {
                int vfsID = vfs.Open(s);
                if (vfsID == -1) {
                    return -1;
                }
                pcb.deviceIds[i] = vfsID;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void Close(int userId) {
        PCB pcb = scheduler.getCurrentlyRunning();
        int vfsID = pcb.deviceIds[userId];
        if (vfsID != -1) {
            vfs.Close(vfsID);
            pcb.deviceIds[userId] = -1; //release slot
        }
    }

    @Override
    public byte[] Read(int userId, int size) {
        PCB pcb = scheduler.getCurrentlyRunning();
        int vfsID = pcb.deviceIds[userId];
        return vfs.Read(vfsID, size);
    }

    @Override
    public void Seek(int id, int to) {
        PCB pcb = scheduler.getCurrentlyRunning();
        int vfsID = pcb.deviceIds[id];
        vfs.Seek(vfsID, to);
    }

    @Override
    public int Write(int userId, byte[] data) {
        PCB pcb = scheduler.getCurrentlyRunning();
        int vfsID = pcb.deviceIds[userId];
        return vfs.Write(vfsID, data);
    }

    private void closeAll(PCB pcb) {
        for (int i = 0; i < pcb.deviceIds.length; i++) {
            if (pcb.deviceIds[i] != -1) {
                vfs.Close(pcb.deviceIds[i]);
                pcb.deviceIds[i] = -1;
            }
        }
    }

    private void addProcess(PCB pcb) {
        pidTable.put(pcb.pid, pcb);
        //register for scheduler queue
        scheduler.addProcess(pcb);
    }

    public PCB getCurrentProcess(){
        return scheduler.currentlyRunning;
    }

    private void scheduleNextProcess() {
        scheduler.currentlyRunning = scheduler.getNextProcess();
        if (scheduler.currentlyRunning != null) {
            scheduler.currentlyRunning.start();
        }
    }

    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        //return scheduler.CreateProcess(up, priority);
        PCB pcb = new PCB(up, priority);
        addProcess(pcb);
        return pcb.pid;
    }

    private void SwitchProcess() {
        PCB current = scheduler.currentlyRunning;
        if (current != null && current.isDone()) {
            pidTable.remove(current.pid);
        }
        scheduleNextProcess();
    }

    private void SendMessage(KernelMessage km) {
        //make a copy constructor
        KernelMessage copy = new KernelMessage(km);
        copy.senderPid = getPCB().pid;

        //find the target's PCB
        PCB target = pidTable.get(copy.receiverPid);
        if (target != null) {
            target.messageQueue.add(copy);

            if(target.isWaitingForMessage){
                scheduler.makeRunnable(target);
            }
        }
    }

    private KernelMessage WaitForMessage() {
        PCB current = scheduler.getCurrentlyRunning();

        //if message is here, just return it
        if(!current.messageQueue.isEmpty()){
            KernelMessage msg = current.messageQueue.removeFirst();
            OS.retVal = msg;
            return msg;
        }

        //if message is not here : wait
        current.isWaitingForMessage = true;
        scheduler.blockCurrentProcess();

        //return null right after block
        OS.retVal = null;
        return null;
    }

    private int GetPidByName(String name) {
        for (PCB pcb : pidTable.values()) {
            if (pcb.getName().equals(name)) {
                return pcb.getPid();
            }
        }
        return -1;
    }

    private void GetMapping(int virtualPage) {
    }

    private int AllocateMemory(int size) {
        return 0; // change this
    }

    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
    }

}