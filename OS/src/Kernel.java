import java.util.HashMap;
import java.util.LinkedList;

public class Kernel extends Process implements Device {
    private Scheduler scheduler;
    private VFS vfs = new VFS(); //all device-related calls goes to VFS
    private HashMap<Integer, PCB> pidTable = new HashMap<>();
    private int nextPid = 1;
    private PCB currentProcess;
    private boolean[] physicalUsed = new boolean[1024];
    private LinkedList<Integer> freeList = new LinkedList<>();

    public Kernel() {
        super();
        scheduler = new Scheduler();

        //initialize free list
        for (int i = 0; i < 1024; i++) {
            freeList.add(i);
        }
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

                // Messages
                case GetPIDByName -> OS.retVal = GetPidByName((String) OS.parameters.get(0));
                case SendMessage -> {
                    SendMessage((KernelMessage) OS.parameters.get(0));
                    OS.retVal = 0;
                }
                case WaitForMessage -> OS.retVal = WaitForMessage();

                // Memory
                case GetMapping -> {GetMapping((int) OS.parameters.get(0)); OS.retVal = 0;}
                case AllocateMemory -> OS.retVal = AllocateMemory((int) OS.parameters.get(0));
                case FreeMemory -> OS.retVal = FreeMemory((int) OS.parameters.get(0), (int) OS.parameters.get(1));

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
        FreeAllMemory(pcb);
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
        PCB pcb = new PCB(up, priority);
        addProcess(pcb);

        // IdleProcess는 오직 한 번만 등록
        if (up instanceof IdleProcess) {
            if (scheduler.getIdleProcess() == null) {
                scheduler.setIdleProcess(pcb);
                System.out.println("[DEBUG] IdleProcess registered: PID=" + pcb.pid);
            } else {
                System.out.println("[DEBUG] IdleProcess already exists, skipping re-registration");
            }
        }

        return pcb.pid;
    }

    private void SwitchProcess() {
        PCB current = scheduler.currentlyRunning;
        if (current != null && current.isDone()) {
            pidTable.remove(current.pid);
        }
        Hardware.clearTLB();
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
                target.isWaitingForMessage = false;
            }
            scheduler.SwitchProcess();
        }
        OS.retVal = 0;
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
        if (name == null) return -1;

        System.out.println("[DEBUG] pidTable keys: " + pidTable.keySet());
        for (PCB pcb : pidTable.values()) {
            if (pcb.getName() != null && pcb.getName().equalsIgnoreCase(name)) {
                System.out.println("[DEBUG] Found " + name + " -> PID=" + pcb.getPid());
                return pcb.getPid();
            }
        }

        System.out.println("[DEBUG] Not found: " + name);
        return -1;
    }

    private void GetMapping(int virtualPage) {
        //bring the currently running process
        PCB pcb = scheduler.getCurrentlyRunning();

        //check virtual page availability
        //check TLB hit
        if (virtualPage < 0 || virtualPage >= pcb.pageTable.length) {
            System.out.println("Segfault : invalid virtual page");
            Exit();
            scheduler.SwitchProcess();
            return;
        }

        //if the mapping already exists, just update TLB
        int pp = pcb.pageTable[virtualPage];
        if (pp != -1) {
            Hardware.updateTLB(virtualPage, pp);
            return;
        }

        //find the empty physical page
        //if TLB miss
        int physicalPage = findFreePhysicalPage();
        if (physicalPage == -1) {
            System.out.println("Segfault : no free physical page");
            Exit();
            scheduler.SwitchProcess();
            return;
        }

        //add mapping
        pcb.pageTable[virtualPage] = physicalPage;
        physicalUsed[physicalPage] = true;

        //update TLB
        Hardware.updateTLB(virtualPage, physicalPage);
    }

    private int findFreePhysicalPage() {
        if (freeList.isEmpty()) return -1;
        return freeList.removeFirst();
    }

    private void freePhysicalPage(int pp) {
        if (pp >= 0 && pp < 1024) {
            freeList.add(pp);
        }
    }

    private int AllocateMemory(int size) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null || size <= 0) return -1;

        //set pageSize as 1024
        int pageSize = 1024;
        //size is not multiple of 1024
        if (size % pageSize != 0) {
            return -1;
        }

        int numPages = size / pageSize;

        //find the empty consecutive virtual page area
        int startVirtual = -1;
        int count = 0;
        for (int i = 0; i < pcb.pageTable.length; i++) {
            if (pcb.pageTable[i] == -1) {
                if (count == 0) startVirtual = i;
                count++;
                if (count == numPages) break;
            } else {
                count = 0;
                startVirtual = -1;
            }
        }

        //can't find enough space
        if (startVirtual == -1 || count == numPages) {
            return -1;
        }

        //physical page allocation and registration for mapping
        for (int i = 0; i < numPages; i++) {
            int pp = findFreePhysicalPage();
            if (pp == -1) {
                FreeMemory(startVirtual * pageSize, i * pageSize);
                return -1;
            }
            //mapping the found physical page into virtual
            pcb.pageTable[startVirtual + i] = pp;
            //alert for using
            physicalUsed[pp] = true;
            Hardware.updateTLB(startVirtual + i, pp);
        }

        return startVirtual * pageSize; //return virtual address by byte
    }

    private boolean FreeMemory(int pointer, int size) {
        PCB pcb = scheduler.getCurrentlyRunning();

        int pageSize = 1024;
        if (pointer % pageSize != 0 || size % pageSize != 0) {
            return false;
        }

        int startVirtual = pointer / pageSize;
        int numPages = size / pageSize;

        if (startVirtual + numPages > pcb.pageTable.length) {
            return false;
        }

        for (int i = 0; i < numPages; i++) {
            int vp = startVirtual + i;
            int pp = pcb.pageTable[vp];
            if (pp != -1) {
                pcb.pageTable[vp] = -1;
                freePhysicalPage(pp);
            }
        }

        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
        if (currentlyRunning == null) return;

        for (int vp = 0; vp < currentlyRunning.pageTable.length; vp++) {
            int pp = currentlyRunning.pageTable[vp];
            if (pp != -1) {
                //eliminate virtual page mapping
                currentlyRunning.pageTable[vp] = -1;

                //deallocate physical page
                freePhysicalPage(pp);
            }
        }
    }

}