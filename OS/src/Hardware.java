public class Hardware {
    //static member and static method
    //singleton (one instance)
    //able to access from both userland and kernel
    static int[][] TLB = new int[2][2];
    private static byte[] memory = new byte[1024 * 1024];

    //static initializer
    static {
        for(int i = 0; i < 2; i++) {
            TLB[i][0] = -1;
            TLB[i][1] = -1;
        }
    }

    //address = virtual address
    //LOAD
    public static byte Read(int address) {
        int pageSize = 1024;
        int virtualPageNum = address / pageSize;
        int pageOffset = address % pageSize;

        //find mapping in TLB //hit
        int physicalPageNum = -1;
        for (int i = 0; i < 2; i++) {
            if (TLB[i][0] == virtualPageNum) {
                physicalPageNum = TLB[i][1];
                break;
            }
        }

        //if there is no mapping, ask to OS
        if (physicalPageNum == -1) {
            OS.GetMapping(virtualPageNum);

            //find again (it must be here)
            for (int i = 0; i < 2; i++) {
                if (TLB[i][0] == virtualPageNum) {
                    physicalPageNum = TLB[i][1];
                    break;
                }
            }
        }

        int physicalAddress = (physicalPageNum * pageSize) + (pageOffset);
        return memory[physicalAddress];
    }

    //STORE
    public static void Write(int address, byte value) {
        int pageSize = 1024;
        int virtualPageNum = address / pageSize;
        int pageOffset = address % pageSize;

        //find mapping in TLB
        int physicalPageNum = -1;
        for (int i = 0; i < 2; i++) {
            if (TLB[i][0] == virtualPageNum) {
                physicalPageNum = TLB[i][1];
                break;
            }
        }

        //if there is no mapping, ask to OS
        if (physicalPageNum == -1) {
            OS.GetMapping(virtualPageNum);

            //find again (it must be here)
            for (int i = 0; i < 2; i++) {
                if (TLB[i][0] == virtualPageNum) {
                    physicalPageNum = TLB[i][1];
                    break;
                }
            }
        }

        int physicalAddress = (physicalPageNum * pageSize) + (pageOffset);
        memory[physicalAddress] = value;
    }

    //choose one of the TLB entries and save into (VP, PP) shape
    public static void updateTLB(int virtualPageNum, int physicalPageNum) {
        //choose one entry by random (0 or 1)
        int index = (int) (Math.random() * 2);

        //save the new mapping into chosen entry
        TLB[index][0] = virtualPageNum;
        TLB[index][1] = physicalPageNum;
    }

    public static void clearTLB() {
        for (int i = 0; i < 2; i++) {
            TLB[i][0] = -1;
            TLB[i][1] = -1;
        }
    }

    public static byte[] readPhysicalMemory(int physicalPageNum) {
        int pageSize = 1024;
        byte[] buf = new byte[pageSize];

        int start = physicalPageNum * pageSize;
        for (int i = 0; i < pageSize; i++) {
            buf[i] = memory[start + i];
        }

        return buf;
    }

    public static void writePhysicalMemory(int physicalPageNum, byte[] data) {
        int pageSize = 1024;
        int start = physicalPageNum * pageSize;

        for (int i = 0; i < pageSize; i++) {
            memory[start + i] = data[i];
        }
    }

}
