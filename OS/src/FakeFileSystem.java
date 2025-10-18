import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class FakeFileSystem implements Device {
    private RandomAccessFile[] files = new RandomAccessFile[10];

    @Override
    public int Open(String filename) {
        //근데 왜 얘만 s가 아니라 filename을 받아?
        //그래도 implementation에는 문제 없나?
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        try {
            for (int i = 0; i < files.length; i++) {
                if (files[i] == null) {
                    files[i] = new RandomAccessFile(filename, "rw"); //read || write
                    return i;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void Close(int id) {
        try {
            if (files[id] != null) {
                files[id].close();
                files[id] = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        try {
            byte[] buffer = new byte[size];
            int bytesRead = files[id].read(buffer);
            if (bytesRead == -1) {
                return new byte[0]; // EOF
            }
            if (bytesRead < size) {
                byte[] actual = new byte[bytesRead];
                System.arraycopy(buffer, 0, actual, 0, bytesRead);
                return actual;
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void Seek(int id, int to) {
        try {
            files[id].seek(to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        try {
            files[id].write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
