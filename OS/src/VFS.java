public class VFS implements Device {
    private Device[] devices = new Device[10];
    private int[] ids = new int[10];
    private RandomDevice random = new RandomDevice();
    private FakeFileSystem fileSystem = new FakeFileSystem();

    @Override
    public int Open(String s) {
        String[] parts = s.split(" ", 2);
        String DeviceName = parts[0];
        String rest = parts.length > 1 ? parts[1] : "";

        Device dev;
        //but this makes devices 'limited'... isn't it?
        if (DeviceName.equals("random")) {
            dev = random;
        } else if (DeviceName.equals("file")) {
            dev = fileSystem;
        } else {
            return -1;
        }

        int deviceID = dev.Open(rest);
        if (deviceID == -1) {
            return -1;
        }

        for (int i = 0; i < devices.length; i++) {
            if (devices[i] == null) {
                devices[i] = dev;
                ids[i] = deviceID;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void Close(int id) {
        if (devices[id] != null) {
            devices[id].Close(ids[id]);
            devices[id] = null;
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        return devices[id].Read(ids[id], size);
    }

    @Override
    public void Seek(int id, int to) {
        devices[id].Seek(ids[id], to);
    }

    @Override
    public int Write(int id, byte[] data){
        return devices[id].Write(ids[id], data);
    }
}
