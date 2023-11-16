import java.io.*;

public class LocalInputOutput {
    static int row;
    static int col;

    static void save(final String path, final int[][]data) throws IOException {
        int[][] drawing = data;
        DataOutputStream out = new DataOutputStream(new FileOutputStream(path));


        out.writeInt(drawing.length);
        out.writeInt(drawing[0].length);
        for (int i = 0; i < drawing.length; i++) {
            for (int j = 0; j < drawing[i].length; j++) {
                out.writeInt(drawing[i][j]);
            }
        }
        out.flush();
        out.close();
    }
    static int[][] importFile(final String path) throws IOException {
        DataInputStream in  = new DataInputStream(new FileInputStream(path));
        row = in.readInt();
        col = in.readInt();

        int[][] drawing = new int[row][col];
        for (int i = 0; i < drawing.length; i++) {
            for (int j = 0; j < drawing[i].length; j++) {
                drawing[i][j] = in.readInt();
            }
        }

        in.close();
        return drawing;
    }
}