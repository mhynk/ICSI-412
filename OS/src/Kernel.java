import java.util.HashMap;
import java.util.LinkedList;

public class Kernel extends Process implements Device {
    private Scheduler scheduler;
    private VFS vfs = new VFS(); //all device-related calls goes to VFS
    private HashMap<Integer, PCB> pidTable = new HashMap<>();
    private int nextPid = 1;
    private PCB currentProcess;
    private boolean[] physicalUsed = new boolean[1024];
    //private LinkedList<Integer> freeList = new LinkedList<>();
    private int swapFile;
    private int nextSwapPage;

    public Kernel() {
        super();
        scheduler = new Scheduler();

        //open the swap file on startup
        swapFile = vfs.Open("swapFile.sys");
        nextSwapPage = 0;
    }

    private int allocateSwapPageNumber() {
        //page = 1024 bytes
        //not reusing swap file
        return nextSwapPage++;
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
            System.out.println("Segfault : invalid virtual page" + virtualPage);
            Exit();
            scheduler.SwitchProcess();
            return;
        }

        //check if the virtual page had been allocated
        VirtualToPhysicalMapping mapping = pcb.pageTable[virtualPage];
        if (mapping == null) {
            System.out.println("Segfault : accessing unallocated virtual page");
            Exit();
            scheduler.SwitchProcess();
            return;
        }

        //if physical page is already exist (only TLB miss)
        if (mapping.physicalPage != -1) {
            Hardware.updateTLB(virtualPage, mapping.physicalPage);
            return;
        }

        //find the free frame if there is no physical page
        int physicalPage = findFreePhysicalPage();
        //if there is no free frame, swap out other process page, make frame
        if (physicalPage == -1) {
            physicalPage = evictPageAndGetFrame(pcb);
            if (physicalPage == -1) {
                System.out.println("Segfault : no free physical page and no victim to evict");
                Exit();
                scheduler.SwitchProcess();
                return;
            }
        }

        //if this virtual page had saved to disk, swap in
        if (mapping.diskPage != -1) {
            //swap in : from disk to physical page
            byte[] buf = new byte[1024];

            //move to the exact diskPage location and read
            vfs.Seek(swapFile, mapping.diskPage * 1024);
            buf = vfs.Read(swapFile, 1024);

            //put into physical memory
            Hardware.writePhysicalMemory(physicalPage, buf);

            mapping.diskPage = -1;
        } else {
            //if it is the first access, even not exist in disk,
            byte[] zero = new byte[1024]; //initialized to 0
            Hardware.writePhysicalMemory(physicalPage, zero);
        }

        //add physical page number to mapping
        mapping.physicalPage = physicalPage;
        physicalUsed[physicalPage] = true;

        //update TLB
        Hardware.updateTLB(virtualPage, physicalPage);
    }

    //swap out the one of the page from other process that exclude the current process pcb
    //and return the physical frame number
    private int evictPageAndGetFrame(PCB currentPCB) {
        PCB victim = scheduler.getRandomProcess(currentPCB);
        if (victim == null) {
            return -1;
        }

        //randomly choose the virtual page that have physical page in the process
        int victimVP = chooseRandomAllocatedPage(victim);
        if (victimVP == -1) {
            return -1;
        }

        VirtualToPhysicalMapping vMap = victim.pageTable[victimVP];
        int physical = vMap.physicalPage;

        if (physical == -1) {
            return -1;
        }

        //allocate new disk page number
        int diskPage = allocateSwapPageNumber();
        vMap.diskPage = diskPage;

        //read the physical memory and sae to swapFile
        byte[] buf = Hardware.readPhysicalMemory(physical); //page size = 1024

        vfs.Seek(swapFile, diskPage * 1024);
        vfs.Write(swapFile, buf);

        //this page is disappeared in physical memory in victim
        vMap.physicalPage = -1;

        //return physical fram to free list
        freePhysicalPage(physical);

        //and give this frame to new requester
        return physical;
    }

    //pick virtual page which have real physical page from victim process randomly and return
    private int chooseRandomAllocatedPage(PCB victim) {
        java.util.ArrayList<Integer> candidates = new java.util.ArrayList<>();

        for (int vp = 0; vp < victim.pageTable.length; vp++) {
            VirtualToPhysicalMapping mapping = victim.pageTable[vp];
            if (mapping != null && mapping.physicalPage != -1) {
                candidates.add(vp);
            }
        }

        if (candidates.isEmpty()) {
            return -1;
        }

        java.util.Random rand = new java.util.Random();
        return candidates.get(rand.nextInt(candidates.size()));
    }

    private int findFreePhysicalPage() {
        for (int i = 0; i < physicalUsed.length; i++) {
            if (!physicalUsed[i]) {
                physicalUsed[i] = true;
                return i;
            }
        }
        return -1;
    }

    private void freePhysicalPage(int pp) {
        if (pp >= 0 && pp < physicalUsed.length) {
            physicalUsed[pp] = false;
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
            if (pcb.pageTable[i] == null) {
                if (count == 0) startVirtual = i;
                count++;
                if (count == numPages) break;
            } else {
                count = 0;
                startVirtual = -1;
            }
        }

        //can't find enough space
        if (startVirtual == -1 || count != numPages) { //?? == or !=
            return -1;
        }

        //lazy allocation
        for (int i = 0; i < numPages; i++) {
            //mapping the found physical page into virtual
            pcb.pageTable[startVirtual + i] = new VirtualToPhysicalMapping();
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
            VirtualToPhysicalMapping mapping = pcb.pageTable[vp];

            if (mapping == null) {
                //never allocated
                continue;
            }

            //if physical page exists, free it
            if (mapping.physicalPage != -1) {
                freePhysicalPage(mapping.physicalPage);
            }

            //set pageTable slot to null (free the mapping)
            pcb.pageTable[vp] = null;
        }

        return true;
    }

    private void FreeAllMemory(PCB pcb) {
        if (pcb == null) return;

        for (int vp = 0; vp < pcb.pageTable.length; vp++) {
            VirtualToPhysicalMapping mapping = pcb.pageTable[vp];

            //never did allocated
            if (mapping == null) {
                continue;
            }

            //free physical page
            if (mapping.physicalPage != -1) {
                freePhysicalPage(mapping.physicalPage);
            }

            //free mapping too
            pcb.pageTable[vp] = null;
        }
    }
}

