public class DeviceTest extends UserlandProcess{

    @Override
    public void main() {
        int randomId = OS.Open("random 123");
        cooperate();
        byte[] randData = OS.Read(randomId, 5);
        cooperate();

        int fileId = OS.Open("file test.txt");
        cooperate();
        OS.Write(fileId, "Hello".getBytes());
        cooperate();
        OS.Seek(fileId, 0);
        cooperate();
        byte[] fileData = OS.Read(fileId, 5);
        cooperate();

        System.out.println(new String(fileData));
        OS.Exit();
    }
}


