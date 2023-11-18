import java.io.*;

public class SaveAndLoad {
    static int row;
    static int col;

    /**
     * Saves the drawing data to a file.
     *
     * @param path The file path to save the data to.
     * @param data The drawing data to be saved.
     * @throws IOException If an I/O error occurs while saving the data.
     */
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

    /**
     * Loads a drawing from a file.
     * 
     * @param path the path of the file to load
     * @return the loaded drawing as a 2D array of integers
     * @throws IOException if an I/O error occurs while reading the file
     */
    static int[][] load(final String path) throws IOException {
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