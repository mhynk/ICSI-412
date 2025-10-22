import java.time.Clock;
import java.util.*;

public class Scheduler {
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private Timer timer = new Timer();
    public PCB currentlyRunning; //tracker
    private List<PCB> sleeping = new ArrayList<>(); //separate list
    // IdleProcess 전용 저장
    private PCB idleProcessPCB;
    //private PCB current;
    //private Kernel kernel;

    public PCB getCurrentlyRunning() {
        return currentlyRunning;
    }

    //for test
    public void setCurrentlyRunning(PCB pcb) {
        this.currentlyRunning = pcb;
    }

    public Scheduler() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(currentlyRunning != null) {
                    currentlyRunning.requestStop();
                }
            }
        }, 250, 250);
    }

    public PCB getNextProcess() {
        if (!realTimeQueue.isEmpty()) {
            return realTimeQueue.removeFirst();
        }
        else if (!interactiveQueue.isEmpty()) {
            return interactiveQueue.removeFirst();
        }
        else if (!backgroundQueue.isEmpty()) {
            return backgroundQueue.removeFirst();
        }
        else {
            return idleProcessPCB;
        }
    }

    public void setIdleProcess (PCB pcb) {
        this.idleProcessPCB = pcb;
    }

    public PCB getIdleProcess() {
        return idleProcessPCB;
    }

    public void blockCurrentProcess() {
        if (currentlyRunning != null) {
            currentlyRunning.isWaitingForMessage = true;
            currentlyRunning = null;
            SwitchProcess();
        }
    }

    public void addProcess(PCB pcb) {
        switch (pcb.getPriority()){
            case realtime -> realTimeQueue.add(pcb);
            case interactive -> interactiveQueue.add(pcb);
            case background -> backgroundQueue.add(pcb);
        }

        if(currentlyRunning == null) {
            SwitchProcess();
        }
    }

    public void makeRunnable(PCB p) {
        p.isWaitingForMessage = false;
        switch(p.getPriority()) {
            case realtime -> realTimeQueue.add(p);
            case interactive -> interactiveQueue.add(p);
            case background -> backgroundQueue.add(p);
        }
    }

    public int CreateProcess(UserlandProcess up, OS.PriorityType p) {
        PCB pcb = new PCB(up, p);

        switch(p) {
            case realtime -> realTimeQueue.add(pcb);
            case interactive -> interactiveQueue.add(pcb);
            case background -> backgroundQueue.add(pcb);
        }

        if (currentlyRunning == null) {
            SwitchProcess();
        }

        return pcb.pid;
    }

    public void Sleep(int mills) {
        if (currentlyRunning != null) {
            //**Does using systemUTC makes debugging difficult?
            long wakeTime = Clock.systemUTC().millis() + mills; //wake time = current time + sleep time
            currentlyRunning.setWakeTime(wakeTime); //to PCB

            sleeping.add(currentlyRunning); //standby

            currentlyRunning = null; //clear out
            SwitchProcess();
        }
    }

    public void SwitchProcess() {
        wakeSleepProcess();

        // demote / requeue currently running process if still alive
        if (currentlyRunning != null && !currentlyRunning.isDone()) {
            OS.PriorityType p = currentlyRunning.getPriority();
            currentlyRunning.incrementTimeout();

            if (currentlyRunning.getTimeoutCount() >= 5) {
                switch (p) {
                    case realtime -> currentlyRunning.setPriority(OS.PriorityType.interactive);
                    case interactive -> currentlyRunning.setPriority(OS.PriorityType.background);
                    case background -> {}
                }
                currentlyRunning.resetTimeout();
            }

            switch (currentlyRunning.getPriority()) {
                case realtime -> realTimeQueue.add(currentlyRunning);
                case interactive -> interactiveQueue.add(currentlyRunning);
                case background -> backgroundQueue.add(currentlyRunning);
            }
        }

        // now pick next process
        Random rand = new Random();
        currentlyRunning = null;

        if (!realTimeQueue.isEmpty()) {
            int roll = rand.nextInt(10); // 0~9
            if (roll < 6) {                            // 60%
                currentlyRunning = realTimeQueue.removeFirst();
            } else if (roll < 9 && !interactiveQueue.isEmpty()) { // 30%
                currentlyRunning = interactiveQueue.removeFirst();
            } else if (!backgroundQueue.isEmpty()) {   // 10%
                currentlyRunning = backgroundQueue.removeFirst();
            } else if (!interactiveQueue.isEmpty()) {  // fallback
                currentlyRunning = interactiveQueue.removeFirst();
            } else {                                   // fallback
                currentlyRunning = realTimeQueue.removeFirst();
            }
        } else if (!interactiveQueue.isEmpty()) {
            int roll = rand.nextInt(4); // 0~3
            if (roll < 3) {             // 75%
                currentlyRunning = interactiveQueue.removeFirst();
            } else if (!backgroundQueue.isEmpty()) {
                currentlyRunning = backgroundQueue.removeFirst();
            } else {                    // fallback
                currentlyRunning = interactiveQueue.removeFirst();
            }
        } else if (!backgroundQueue.isEmpty()) {
            currentlyRunning = backgroundQueue.removeFirst();
        } else if (idleProcessPCB != null) {
            currentlyRunning = idleProcessPCB;
        }

        //debugging log
        if (currentlyRunning != null) {
           System.out.println("[SwitchProcess] Picked PID=" +
                    currentlyRunning.getPid() + " (" + currentlyRunning.getPriority() + ")");
        } else {
            System.out.println("[SwitchProcess] Picked null (no process)");
        }
    }

    private void wakeSleepProcess() {
        long now = Clock.systemUTC().millis();
        Iterator<PCB> it = sleeping.iterator();

        // wake sleeping processes
        while (it.hasNext()) {
            PCB p = it.next();
            if (p.getWakeTime() <= now) {
                switch (p.getPriority()) {
                    case realtime -> realTimeQueue.add(p);
                    case interactive -> interactiveQueue.add(p);
                    case background -> backgroundQueue.add(p);
                }
                it.remove();
            }
        }
    }

    public void exitCurrentProcess() {
        if (currentlyRunning != null) {
            //currentlyRunning.timeoutCount = 0;
            System.out.println("[Exit] Terminating PID=" + currentlyRunning.getPid());
            //currentlyRunning.stop();
            currentlyRunning = null;

        }
        SwitchProcess();

    }
}
